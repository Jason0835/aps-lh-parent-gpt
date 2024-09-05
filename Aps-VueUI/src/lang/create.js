const path = require('path');
const fs = require("fs").promises


// 假设你的JSON文件位于'jsonFiles'目录中
const jsonFilesDir = './zh';

async function readAndFlattenJsonFiles(outputPath) {
  // 读取目录下的所有文件
  const files = await fs.readdir(jsonFilesDir);
  const flattenedData = {};

  // 遍历每个文件
  for (const file of files) {
    // 只处理.json文件
    // console.log(file)
    if (path.extname(file) === '.js' && file !== 'index.js') {

      const filePath = path.join(jsonFilesDir, file);
      // 读取文件内容
      let content = await fs.readFile(filePath, 'utf8');
      // console.log(content);
      let data= {};
      content = content.replace('export default', `data['${file.replace('.js', '')}'] = ` );
      eval(content)

      // content = content.replace('};', "}");

      // 解析JSON
      console.log(data)
      // const data = JSON.parse(content);

      // 递归地遍历并扁平化JSON对象
      flattenObject(data, '', flattenedData);
    }
  }

  // 将扁平化的数据写入到一个新的JSON文件中
  await fs.writeFile(outputPath, JSON.stringify(flattenedData, null, 2));
}

// 递归函数，用于扁平化对象
function flattenObject(obj, prefix = '', result = {}) {
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const value = obj[key];
      const newKey = prefix ? `${prefix}.${key}` : key; // 构建新的键

      if (typeof value === 'object' && value !== null) {
        // 如果值是对象，则递归调用
        flattenObject(value, newKey, result);
      } else {
        // 如果值不是对象，则添加到结果中
        result[newKey] = value;
      }
    }
  }
  return result;
}

// 调用函数处理文件，并将结果写入'output.json'
readAndFlattenJsonFiles('output.json').catch(console.error);
