const newman = require('newman');
const fs = require('node:fs');
const collection = require('../Saga_Postman_Collection.json');
const folder = collection.item.filter(f => !f.name.startsWith('Manual')).map(f => f.name);
fs.mkdirSync('evidence', { recursive: true });
let runtimeVariables;
newman.run({ collection, folder, reporters: ['cli', 'json', 'junit'],
  reporter: { json: { export: 'evidence/newman-base.json' }, junit: { export: 'evidence/newman-base.xml' } },
  exportCollection: 'evidence/base-run-variables.json'
}, (error, summary) => {
  if (error) throw error;
  if (runtimeVariables) fs.writeFileSync('evidence/base-variables.json', JSON.stringify(runtimeVariables, null, 2));
  process.exitCode = summary.run.failures.length ? 1 : 0;
}).on('script', (error, args) => {
  if (!error && args.execution?.collectionVariables) {
    runtimeVariables = args.execution.collectionVariables.values.toJSON();
  }
});
