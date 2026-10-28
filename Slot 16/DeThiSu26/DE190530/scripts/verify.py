"""Build first with mvn clean package, then run: python scripts/verify.py.

Uses real SQL Server and all three applications; creates uniquely named test data.
Only processes started by this script are stopped. No database rows are deleted.
"""
import datetime as dt
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import time
import urllib.error
import urllib.request
import uuid

HOME = Path(__file__).resolve().parents[1]
MEDIA = (HOME / 'DE190530UserService').exists()
MASTER, CHILD, GATEWAY = ('user', 'post', 'Media') if MEDIA else ('room', 'reservation', 'Hotel')
MASTER_TITLE, CHILD_TITLE = MASTER.capitalize(), CHILD.capitalize()
PREFIX = 'e2e_' + uuid.uuid4().hex[:10]
PERSON = 'advertiser' if MEDIA else 'guest'
START, END = ('startDate', 'endDate') if MEDIA else ('checkInDate', 'checkOutDate')
COUNT, TOTAL = ('numberOfPosts', 'totalCost') if MEDIA else ('numberOfGuests', 'totalAmount')
RATE, LIMIT = ('ratePerPost', 'maxDailyPosts') if MEDIA else ('pricePerNight', 'capacity')
UNIQUE, KIND = ('username', 'role') if MEDIA else ('roomNumber', 'roomType')
ACTIVE, INACTIVE = ('ACTIVE', 'DEACTIVATED') if MEDIA else ('AVAILABLE', 'MAINTENANCE')
passed = []
processes = []
handles = []


def request(method, path, data=None, port=8080, headers=None, raw=None):
    payload = raw if raw is not None else (json.dumps(data).encode() if data is not None else None)
    req = urllib.request.Request('http://localhost:' + str(port) + path, data=payload,
                                 method=method, headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        body = response.read().decode()
        try:
            body = json.loads(body)
        except json.JSONDecodeError:
            pass
        return response.status, body, response.headers


def expect(name, method, path, data=None, http=200, status=1, **kwargs):
    actual, body, headers = request(method, path, data, **kwargs)
    assert actual == http, (name, actual, body)
    assert isinstance(body, dict) and body['status'] == status, (name, body)
    assert re.fullmatch(r'\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ', body['timestamp']), (name, body)
    assert 'application/json' in headers.get('Content-Type', ''), (name, headers)
    if status != 1:
        assert body['data'] is None and body['message'], (name, body)
    passed.append(name)
    return body['data']


def master_body(value):
    if MEDIA:
        return dict(username=value, email='creator@example.com', role='CREATOR',
                    ratePerPost=500000, maxDailyPosts=3, level=1)
    return dict(roomNumber=value, roomType='DOUBLE', pricePerNight=850000, capacity=3, floor=1)


def child_body(master_id):
    return {PERSON+'Name': PREFIX+' Shop', PERSON+'Email': 'ads@example.com',
            PERSON+'Phone': '0901234567', START: '25/07/2026', END: '28/07/2026',
            COUNT: 2, MASTER+'Id': master_id}


def run_checks():
    masters, children = '/api/'+MASTER+'s', '/api/'+CHILD+'s'
    source = master_body(PREFIX)
    first = expect('create master', 'POST', masters, source, http=201)
    mid = first[MASTER+'Id']
    assert first['status'] == ACTIVE
    expect('duplicate master', 'POST', masters, source, http=226, status=3)
    other = expect('create second master', 'POST', masters, master_body(PREFIX+'_2'), http=201)
    oid = other[MASTER+'Id']
    mpath = masters+'/'+str(mid)
    expect('duplicate update', 'PUT', masters+'/'+str(oid), {UNIQUE: PREFIX}, http=226, status=3)
    assert expect('duplicate update leaves data intact', 'GET', masters+'/'+str(oid))[UNIQUE] == PREFIX+'_2'
    bad_master = [{}, {RATE: 0}, {LIMIT: 11}, {LIMIT: 0}, {KIND:'INVALID'}, {UNIQUE: ' '},
                  {UNIQUE: 'x'*101}, {RATE: 1.123}, {'status':'INVALID'}]
    if MEDIA:
        bad_master += [{'email':'invalid'}, {'level':0}, {'email':'a'*101}]
    else:
        bad_master += [{'floor':0}]
    for i, bad in enumerate(bad_master):
        # Empty patch is valid, but an empty create request must fail.
        expect('master required fields' if not bad else 'master invalid '+str(i),
               'POST' if not bad else 'PUT', masters if not bad else mpath,
               bad, http=406, status=2)
    expect('master partial update', 'PUT', mpath, {LIMIT: 4})
    changed = expect('master retained omitted fields', 'GET', mpath)
    assert changed[UNIQUE] == PREFIX and changed[LIMIT] == 4
    expect('master negative page','GET',masters+'?page=-1',http=406,status=2)
    expect('master size too large','GET',masters+'?size=101',http=406,status=2)
    expect('master zero size','GET',masters+'?size=0',http=406,status=2)
    expect('master invalid filter','GET',masters+'?status=INVALID',http=406,status=2)
    expect('master invalid kind','GET',masters+'?'+KIND+'=INVALID',http=406,status=2)
    page = expect('master paginated filters','GET',masters+'?size=1&'+KIND+'='+source[KIND]+'&status='+ACTIVE)
    assert page['size']==1 and page['page']==0 and page['first'] and page['totalElements'] >= 2
    assert page['content'][0][KIND] == source[KIND]
    for method in ['GET','PUT','DELETE']:
        expect('missing master '+method,method,masters+'/2147483647',{} if method=='PUT' else None,http=404,status=4)
    expect('invalid path ID','GET',masters+'/abc',http=406,status=2)
    expect('malformed JSON','POST',masters,http=406,status=2,raw=b'{')

    source_child = child_body(mid)
    created = expect('create child via real feign','POST',children,source_child,http=201)
    cid = created[CHILD+'Id']
    cpath = children+'/'+str(cid)
    assert created['status'] == ('SCHEDULED' if MEDIA else 'CONFIRMED')
    assert created[TOTAL] == source[RATE]*3
    detail = expect('nested master detail','GET',cpath)
    assert MASTER+'Id' not in detail and detail[MASTER][MASTER+'Id'] == mid
    assert detail[START]=='25/07/2026' and detail[END]=='28/07/2026', detail
    bad_child = [{}, {PERSON+'Email':'invalid'}, {PERSON+'Phone':'x'*21}, {PERSON+'Name':' '},
                 {START:'28/07/2026'}, {END:'25/07/2026'}, {COUNT:0}, {COUNT:5},
                 {START:'31/02/2026'}, {START:'2026-07-25'}, {'status':'INVALID'}]
    for i, bad in enumerate(bad_child):
        body = {} if not bad else {**source_child, **bad}
        expect('child validation '+str(i),'POST',children,body,http=406,status=2)
    expect('feign 404 mapping','POST',children,{**source_child,MASTER+'Id':2147483647},http=404,status=4)
    expect('reject invalid partial dates','PUT',cpath,{END:'24/07/2026'},http=406,status=2)
    after = expect('invalid patch did not persist','GET',cpath)
    assert after[END]=='28/07/2026' and after[TOTAL]==source[RATE]*3
    expect('reject partial count above capacity','PUT',cpath,{COUNT:5},http=406,status=2)
    expect('update current price','PUT',mpath,{RATE:123456})
    updated = expect('partial child update recalculates price','PUT',cpath,{END:'29/07/2026',MASTER+'Id':oid,TOTAL:1})
    assert updated[MASTER+'Id'] == mid and updated[TOTAL] == 123456*4
    assert updated[PERSON+'Name'] == source_child[PERSON+'Name']
    page = expect('child name and status filter','GET',children+'?'+PERSON+'Name='+PREFIX+'&status='+created['status']+'&size=1')
    assert page['content'][0][MASTER][MASTER+'Id']==mid
    assert page['totalElements']==1 and page['totalPages']==1 and page['last']
    for query in ['size=101','page=-1','size=0','status=INVALID','size=abc']:
        expect('child invalid query '+query,'GET',children+'?'+query,http=406,status=2)
    for method in ['GET','PUT','DELETE']:
        expect('missing child '+method,method,children+'/2147483647',{} if method=='PUT' else None,http=404,status=4)
    assert expect('cancel child','DELETE',cpath) is None
    assert expect('cancel is soft delete','GET',cpath)['status']=='CANCELLED'
    assert expect('deactivate master','DELETE',mpath) is None
    assert expect('master soft delete persists','GET',mpath)['status']==INACTIVE
    expect('inactive source rejected','POST',children,source_child,http=400,status=5)
    # Updating an existing booking does not create a new booking or change its source.
    expect('update existing booking after deactivation','PUT',cpath,{PERSON+'Name':PREFIX+' Changed'})

    for svc in [MASTER,CHILD]:
        status, docs, _ = request('GET','/docs/'+svc+'/v3/api-docs')
        assert status==200 and '/api/'+svc+'s' in docs['paths'], (svc,status,docs)
        assert docs['servers'][0]['url']=='/'
        passed.append(svc+' swagger through gateway')
    status, config, _ = request('GET','/v3/api-docs/swagger-config')
    assert status==200 and len(config['urls'])==2, (status,config)
    passed.append('gateway swagger definitions')
    status, _, _ = request('GET','/swagger-ui.html')
    assert status==200
    passed.append('public swagger UI')
    status, _, headers = request('OPTIONS',masters,headers={'Origin':'https://example.test',
        'Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'Content-Type'})
    assert status==200 and headers.get('Access-Control-Allow-Origin')=='*', (status,headers)
    passed.append('gateway CORS preflight')
    for port in [8080,8081,8082]:
        path=children if port==8082 else masters
        status, _, headers = request('GET',path,port=port,headers={'Origin':'https://example.test'})
        assert status==200 and headers.get_all('Access-Control-Allow-Origin')==['*'], (port,status,headers)
        passed.append('single CORS origin header '+str(port))


def main():
    for port in [8080,8081,8082]:
        with socket.socket() as probe:
            assert probe.connect_ex(('localhost',port)) != 0, 'Port '+str(port)+' is in use; stop the other exercise first.'
    logs=HOME/'target'/'e2e-logs'
    logs.mkdir(parents=True,exist_ok=True)
    try:
        for name in [MASTER_TITLE+'Service',CHILD_TITLE+'Service',GATEWAY+'Gateway']:
            folder=HOME/('DE190530'+name)
            jars=list((folder/'target').glob('*.jar'))
            assert len(jars)==1, 'Build first: mvn clean package'
            handle=open(logs/(name+'.log'),'w',encoding='utf-8')
            handles.append(handle)
            processes.append(subprocess.Popen(['java','-jar',str(jars[0])],cwd=folder,stdout=handle,stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0))
        deadline=time.monotonic()+120
        ready=set()
        while len(ready)<3 and time.monotonic()<deadline:
            for i,port in enumerate([8081,8082,8080]):
                assert processes[i].poll() is None, 'Application exited; inspect '+str(logs)
                if port in ready:continue
                try:
                    code,_,_=request('GET','/v3/api-docs',port=port)
                    if code==200:ready.add(port)
                except (OSError,urllib.error.URLError):pass
            time.sleep(0.5)
        assert len(ready)==3, 'Startup timeout; inspect '+str(logs)
        run_checks()
        # A source outage must remain a server error, not be mistaken for a missing ID.
        processes[0].terminate()
        processes[0].wait(timeout=15)
        expect('source outage returns server error','POST','/api/'+CHILD+'s',child_body(1),http=500,status=0)
        report={'case':'media' if MEDIA else 'hotel','passed':len(passed),'checks':passed,
                'timestamp':dt.datetime.now(dt.timezone.utc).isoformat(),'testPrefix':PREFIX}
        (HOME/'target'/'verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
        print(json.dumps(report,indent=2))
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:process.wait(timeout=15)
                except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        for handle in handles:handle.close()


if __name__=='__main__':main()
