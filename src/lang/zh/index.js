const context = require.context("./", true, /\.json$/);
let lang = {};
context.keys().forEach(p => {
  if (p === "./index.js") {
    return;
  }
  const exp = context(p);
  const module = exp.default ? exp.default : exp;
  lang = {...lang,...module}
});
export default lang;
