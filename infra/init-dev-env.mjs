import { randomBytes } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const templatePath = fileURLToPath(new URL('../.env.example', import.meta.url));
const destinationPath = fileURLToPath(new URL('../.env', import.meta.url));

const template = readFileSync(templatePath, 'utf8');
if (!/^DATABASE_PASSWORD=$/m.test(template) || !/^MUSIC_TOKEN_KEYS=$/m.test(template)) {
  throw new Error('The .env.example template has changed; check its secret fields.');
}

const localConfig = template
  .replace(/^DATABASE_PASSWORD=$/m, `DATABASE_PASSWORD=${randomBytes(32).toString('base64')}`)
  .replace(/^MUSIC_TOKEN_KEYS=$/m, `MUSIC_TOKEN_KEYS=v1:${randomBytes(32).toString('base64')}`);

try {
  writeFileSync(destinationPath, localConfig, { flag: 'wx', mode: 0o600 });
  console.log('Created the ignored .env file with unique local secrets.');
} catch (error) {
  if (error.code !== 'EEXIST') throw error;
  console.log('The existing .env file was left unchanged.');
}
