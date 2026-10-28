import { pathToFileURL } from 'node:url';

// Ejecuta `start` solo si el archivo se lanzó directamente con `node archivo.js`
// (y no cuando se importa desde otro módulo, como src/index.js o las pruebas).
export function runIfMain(moduleUrl, start) {
  if (process.argv[1] && moduleUrl === pathToFileURL(process.argv[1]).href) {
    start();
  }
}
