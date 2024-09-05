const context = require.context("./", true, /\.js$/);
const lang = {};
context.keys().forEach(p => {
  if (p === "./index.js") {
    return;
  }
  const exp = context(p);
  const module = exp.default ? exp.default : exp;
  const ps = p.match(/\w+/g);
  let dist = lang;
  for (let i = 0; i < ps.length - 1; i++) {
    const key = ps[i];
    if (i === ps.length - 2) {
      dist[key] = module;
      return;
    }
    if (!dist[ps[i]]) {
      dist[key] = {};
    }
    dist = dist[key];
  }
});
export default lang;
