const newman = require('newman');
const fs = require('node:fs');
const { execFileSync } = require('node:child_process');
let collection = require('../Saga_Extensions_Postman_Collection.json');
const selected = process.argv[2];
if (selected && fs.existsSync('evidence/extension-run-variables.json')) {
  collection.variable = JSON.parse(fs.readFileSync('evidence/extension-run-variables.json', 'utf8')).variable;
}
if (selected && !['recovery', 'shipping', 'shipping-idempotency', 'timeout', 'http-interface', 'outbox', 'choreography'].includes(selected)) {
  throw new Error(`Unknown extension profile: ${selected}`);
}
const enabled = profile => !selected || selected === profile;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
function compose(profile, args, variables = {}) {
  console.log(`Compose ${profile}: ${args.join(' ')}`);
  execFileSync('docker', ['compose', '-f', 'docker-compose.yml', '-f', `docker-compose.${profile}.yml`, ...args],
    { stdio: 'inherit', env: { ...process.env, ...variables } });
}
async function ready() {
  for (const [port, path] of [[8082, 'inventory/products'], [8083, 'payments/wallets'], [8081, 'orders']]) {
    let ok = false;
    for (let i = 0; i < 100; i++) {
      try { if ((await fetch(`http://localhost:${port}/api/${path}`)).ok) { ok = true; break; } } catch {}
      await delay(1000);
    }
    if (!ok) throw new Error(`Service ${port} not ready`);
  }
}
async function run(folder, label) {
  console.log(`\nPostman folder: ${folder}`);
  let runtimeVariables;
  return new Promise((resolve, reject) => newman.run({ collection, folder, reporters: ['cli', 'json', 'junit'],
    reporter: { json: { export: `evidence/extensions-${label}.json` }, junit: { export: `evidence/extensions-${label}.xml` } }
  }, (error, summary) => {
    if (error) return reject(error);
    collection = summary.collection.toJSON();
    // Runtime scopes are separate from the original SDK collection in Newman.
    if (runtimeVariables) collection.variable = runtimeVariables;
    fs.writeFileSync('evidence/extension-run-variables.json', JSON.stringify(collection, null, 2));
    if (summary.run.failures.length) return reject(new Error(`${folder}: ${summary.run.failures.length} failures`));
    resolve();
  }).on('script', (error, args) => {
    if (!error && args.execution?.collectionVariables) {
      runtimeVariables = args.execution.collectionVariables.values.toJSON();
    }
  }));
}
async function start(profile, variables = {}) {
  compose(profile, ['up', '-d', '--no-build'], variables);
  await ready();
  if (profile === 'choreography') {
    // Drain events from an interrupted previous run before taking new snapshots.
    for (let attempt = 0; attempt < 100; attempt++) {
      const existing = await (await fetch('http://localhost:8081/api/orders')).json();
      if (!existing.some(order => order.sagaStatus === 'EXECUTING')) return;
      await delay(500);
    }
    throw new Error('Existing choreography sagas did not finish after consumers restarted');
  }
}
async function main() {
  const failures = JSON.parse(fs.readFileSync('evidence/failure-scenarios.json', 'utf8'));
  collection.variable.find(v => v.key === 'recoveryOrderId').value = String(failures.KB8.order.id);
  if (enabled('recovery')) {
    await start('recovery');
    await run('EXT-4 - Recover KB8', 'recovery');
  }
  if (enabled('shipping')) {
  await start('shipping');
  await run('EXT-1 - Shipping rejection', 'shipping-rejected');
  await run('EXT-1 - Shipping success', 'shipping-success');
  await run('EXT-1 - Shipping idempotency', 'shipping-idempotency');
  }
  if (selected === 'shipping-idempotency') {
    await ready();
    await run('EXT-1 - Shipping idempotency', 'shipping-idempotency');
  }
  if (enabled('timeout')) {
  await start('timeout');
  await run('EXT-2 - Timeout after commit', 'timeout');
  }
  if (enabled('http-interface')) {
  await start('http-interface'); // Also clears the payment timeout profile.
  await run('EXT-6 - HTTP interfaces', 'http-interface');
  }
  if (enabled('outbox')) {
  await start('outbox', { OUTBOX_POLL_MS: '60000' });
  await run('EXT-5 - Queue before restart', 'outbox-queued');
  await start('outbox', { OUTBOX_POLL_MS: '500' });
  await run('EXT-5 - Resume after restart', 'outbox-resumed');
  await run('EXT-5 - Outbox payment failure', 'outbox-failure');
  }
  if (enabled('choreography')) {
  await start('choreography');
  await run('EXT-3 - Choreography success', 'choreography-success');
  await run('EXT-3 - Choreography payment failure', 'choreography-failure');
  }
  // Restore the base REST services without deleting any database or Kafka volume.
  execFileSync('docker', ['compose', 'up', '-d', '--no-build'], { stdio: 'inherit' });
  await ready();
  console.log(`${selected || 'All'} extension Postman folders passed; base REST profile restored.`);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
