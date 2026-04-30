const resolve = require.context("@/api", true, /\.js$/);
const apis = {};
resolve.keys().forEach((p) => {
  const exp = resolve(p);
  const module = exp.default ? exp.default : exp;
  const ps = p.match(/\w+/g);
  let dist = apis;
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
export default apis;
