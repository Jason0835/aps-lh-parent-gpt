<script>
export default {
  props: {
    batchSearchColumns: {
      type: Array,
      default: () => null
    }
  },
  data() {
    return {
      conditionOpt: [
        { key: 'and', text: '并且' },
        { key: 'or', text: '或者' }
      ],
      operatorOpt: [
        { key: 'equal', text: '=' },
        { key: 'unequal', text: '!=' },
        { key: 'greater', text: '>' },
        { key: 'greaterEqual', text: '>=' },
        { key: 'less', text: '<' },
        { key: 'lessEqual', text: '<=' },
        { key: 'between', text: '介于' },
        { key: 'exclusive', text: '不包含' },
        { key: 'belong', text: '从属于' },
      ],
      form: {
        firstGroup: [
          {
            condition: '',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
          {
            condition: 'and',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
          {
            condition: 'and',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
        ],
        condition: 'and',
        secondGroup: [
          {
            condition: '',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
          {
            condition: 'and',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
          {
            condition: 'and',
            where: 'equal',
            fields: '',
            column: {},
            value: ''
          },
        ]
      }
    }
  },
  methods: {
    getFormData() {
      return this.form
    },
    resetForm() {
      const that = this
        that.form.condition = 'and'
        const groups = ['firstGroup', 'secondGroup']
        groups.forEach((group) => {
          that.form[group].forEach((item) => {
            item.condition = 'and'
            item.where = 'equal'
            item.fields = ''
            item.column = {}
            item.value = ''
          })
        })
    },
    columnChange(index, group, fields) {
      const column = this.batchSearchColumns.filter((item) => item.prop === fields)
      this.form[group === 1 ? 'firstGroup':'secondGroup'][index].column = column[0] ? { ...column[0]} : {}
    },
    renderInput(item) {
      return (
        <el-input
          v-model={item.value}
          clearable
          placeholder={item.placeholder || "请输入"}
        />
      );
    },
    renderSelect(item) {
      return (
        <t-select
          style="width:100%;"
          v-model={item.value}
          dictType={item.dictType}
          clearable
        ></t-select>
      );
    },
    renderDate(item) {
      return (
        <el-date-picker
          style="width:100%;"
          type={item.dateType || "date"}
          v-model={item.value}
          clearable={item.clearable == false ? false : true}
          value-format={item.valueFormat || "yyyy-MM-dd HH:mm:ss"}
          start-placeholder={"开始时间"}
          end-placeholder={"结束时间"}
        ></el-date-picker>
      );
    },
    renderSearchInput(item) {
      switch (item.type) {
        case "date":
          return this.renderDate(item);
        case "select":
          return this.renderSelect(item);
        case "checkbox":
          return this.renderCheckbox(item);
        case "button":
          return this.renderButton(item);
        default:
          return this.renderInput(item);
      }
    },
    renderLabel(group) {
      return(
        <div class="el-form-item__label-wrap">
          <label class="el-form-item__label" style="min-width:88px;line-height: 20px">{group === 1 ? '第一组' : '第二组'}</label>
        </div>
      )
    },
    renderConditionSelect(item) {
      return (
        <div class="el-form-item__label" style="min-width:68px;line-height: 20px">
          <el-select v-model={item.condition} style="width:76px" placeholder="请选择">
            {
              this.conditionOpt.map((condition) => {
                return(<el-option label={condition.text} value={condition.key} />)
              })
            }
          </el-select>
        </div>
      )
    },
    renderSearchItem(index, group, item) {
      return (
        <div style="display: flex;margin-bottom: 5px">
          {
            index === 0 ? this.renderLabel(group) : this.renderConditionSelect(item)
          }
          <el-select v-model={item.fields} class="mr5" style="flex: 1" onChange={(e) => { this.columnChange(index, group, e) }} placeholder="请选择">
            {
              this.batchSearchColumns.map((column) => {
                return (<el-option label={column.label} value={column.prop} />)
              })
            }
          </el-select>
          <el-select v-model={item.where} class="mr5" style="flex: 1" placeholder="请选择">
            {
              this.operatorOpt.map((operator) => {
                return(<el-option label={operator.text} value={operator.key} />)
              })
            }
          </el-select>
          <div style="flex: 2">
            {item.column.render
              ? item.column.render(item)
              : this.renderSearchInput(item.column)}
          </div>
        </div>
      )
    },
  },
  render() {
    return (
      <el-row gutter={20} type="flex" style="flex-wrap: wrap;align-items: center">
        <el-col xs={24} sm={10} md={10} lg={11} xl={11}>
          {
            this.form.firstGroup.map((item, index) => {
              return this.renderSearchItem(index, 1, item)
            })
          }
        </el-col>
          <el-col xs={24} sm={4} md={4} lg={2} xl={2} style="text-align: center;">
            <el-select v-model={this.form.condition} style="flex: 1;margin-bottom: 3px" placeholder="请选择">
              {
                this.conditionOpt.map((condition) => {
                  return(<el-option label={condition.text} value={condition.key} />)
                })
              }
            </el-select>
        </el-col>
      <el-col xs= {24} sm={10} md={10} lg={11} xl={11}>
        {
          this.form.secondGroup.map((item, index) => {
            return this.renderSearchItem(index, 2, item)
          })
        }
      </el-col>
    </el-row>)
  }
}
</script>

<style scoped>

</style>
