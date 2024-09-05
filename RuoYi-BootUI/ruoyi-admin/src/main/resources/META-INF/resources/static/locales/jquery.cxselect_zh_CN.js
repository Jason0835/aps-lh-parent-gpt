/*!
 * jQuery cxSelect
 * @name jquery.cxselect.js
 * @version 1.4.2
 * @date 2017-09-26
 * @author ciaoca
 * @email ciaoca@gmail.com
 * @site https://github.com/ciaoca/cxSelect
 * @license Released under the MIT license
 */
(function($) {
  // 默认值
  $.cxSelect.defaults = {
    selects: [],            // 下拉选框组
    url: null,              // 列表数据文件路径（URL）或数组数据
    data: null,             // 自定义数据
    emptyStyle: null,       // 无数据状态显示方式
    required: false,        // 是否为必选
    firstTitle: '请选择',    // 第一个选项的标题
    firstValue: '',         // 第一个选项的值
    jsonSpace: '',          // 数据命名空间
    jsonName: 'n',          // 数据标题字段名称
    jsonValue: '',          // 数据值字段名称
    jsonSub: 's'            // 子集数据字段名称
  };

})(jQuery);
