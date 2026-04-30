
const path = require("path");
const fs = require("fs");

const ROOT_DIR = path.resolve(__dirname, "./");
const fileRules = ["**/*.+(properties)"];
const jsonFile = "properties2json.json";

function ascii2native(value) {
    var character = value.split("\\u");
    var native1 = character[0];
    for (var i = 1; i < character.length; i++) {
        var code = character[i];
        native1 += String.fromCharCode(parseInt("0x" + code.substring(0, 4)));
        if (code.length > 4) {
            native1 += code.substring(4, code.length);
        }
    }
    return native1;
}

async function run() {
    console.log("================================>start");
    let zhProperties = {};
    let enProperties = {};
    let curProperties = {};
    let res = {};
    const exist = fs.existsSync(path.resolve(ROOT_DIR, jsonFile));
    if (exist) {
        res = fs.readFileSync(path.resolve(ROOT_DIR, jsonFile), "utf-8");
        res = JSON.parse(res);
    }
    const files = await fs.readdirSync('./');
    console.log(files.length);
    for (const file of files) {
        if (path.extname(file) === '.properties') {
            // console.log(123);
            console.log("开始解析 =========================>", file);
            let count = 0;

            curProperties = zhProperties;

            // let fileContent = file.contents.toString();
            let fileContent = await fs.readFileSync(file, 'utf8');
            fileContent.split("\n").map((line) => {
                if (line.indexOf("=") > -1) {
                    count++;
                    // line = ascii2native(line);
                    const [key, ...value] = line.split("=");
                    // console.log(key, value);
                    curProperties[key.trim()] = value.join("=").trim();
                }
            });
            console.log("词条数量：", count);
            console.log("解析结束 =========================>", file);
            console.log("================================>end");
            //   console.log(zhProperties);
            //   console.log(enProperties);
            let unTranslate = {};
            Object.keys(zhProperties).map((key) => {
                unTranslate[key] = zhProperties[key].trim();
                console.log(key, zhProperties[key].trim());
            });
            // console.log(file);
            
            fs.writeFileSync(
                path.resolve(ROOT_DIR, file.replace('.properties','.json')),
                JSON.stringify(unTranslate, " ", 2)
            );
        }
    }
}

run();