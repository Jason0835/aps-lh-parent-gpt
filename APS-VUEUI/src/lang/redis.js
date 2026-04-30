const path = require('path');
const fs = require("fs").promises


// 假设你的JSON文件位于'jsonFiles'目录中
const jsonFilesDir = './zh';

async function readAndFlattenJsonFiles(outputPath) {
  // 读取目录下的所有文件
  const files = await fs.readdir(jsonFilesDir);
  let data = {};

  // 遍历每个文件
  for (const file of files) {
    // 只处理.json文件
    // console.log(file)
    if (path.extname(file) === '.js' && file !== 'index.js') {

      const filePath = path.join(jsonFilesDir, file);
      // 读取文件内容
      let content = await fs.readFile(filePath, 'utf8');
      // console.log(content);
      // let data= {};
      content = content.replace('export default', `data['${file.replace('.js', '')}'] = ` );
      eval(content)

      data = addTypeFieldRecursively(data);
      // 解析JSON
      console.log(data)


    }
  }

  await fs.writeFile(outputPath, JSON.stringify(data, null, 2));
}

function addTypeFieldRecursively(obj) {
  if (typeof obj !== 'object' || obj === null) {
      // 如果不是对象或null，直接返回
      return obj;
  }

  // 添加 "@type" 字段到当前对象
  if (!obj['@type']) { // 避免重复添加
      // obj['@type'] = 'com.alibaba.fastjson.JSONObject';
      obj = {
        '@type':'com.alibaba.fastjson.JSONObject',
        ...obj
      }
  }

  // 如果对象是数组，则递归处理每个元素
  if (Array.isArray(obj)) {
      for (let i = 0; i < obj.length; i++) {
          obj[i] = addTypeFieldRecursively(obj[i]);
      }
  } else {
      // 如果对象有属性，则递归处理每个属性
      for (let key in obj) {
          if (obj.hasOwnProperty(key)) {
              obj[key] = addTypeFieldRecursively(obj[key]);
          }
      }
  }

  // 返回修改后的对象
  return obj;
}


// 调用函数处理文件，并将结果写入'output.json'
readAndFlattenJsonFiles('output.json').catch(console.error);
