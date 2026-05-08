// Re-exports the @JsExport facade from the bundled Kotlin/JS module under
// the TS-friendly name. The Kotlin facade is `kotestWirespecGeneratorJs`
// (with the `Js` suffix) so it doesn't collide with the Kotlin DSL function
// of the same root name in commonMain.
import { kotestWirespecGeneratorJs } from './wirespec-src-integration-kotest.mjs';

export function kotestWirespecGenerator(seed = 0, registrations) {
    return kotestWirespecGeneratorJs(seed, registrations);
}
