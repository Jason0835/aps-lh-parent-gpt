<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('ui.data.column.cxStock.factoryCode')" prop="factoryCode">
              <el-select v-model="form.factoryCode" filterable clearable :placeholder="$t('common.rule.select')" style="width: 100%">
                <el-option
                  v-for="item in dict.type.biz_factory_name"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.stockDate')" prop="stockDate">
            <el-date-picker
              v-model="form.stockDate"
              type="date"
              :placeholder="$t('ui.frame.placeholder.selectDate')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.embryoCode')" prop="embryoCode">
            <el-input v-model="form.embryoCode" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.stockNum')" prop="stockNum">
            <el-input-number v-model="form.stockNum" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.overTimeStock')" prop="overTimeStock">
            <el-input-number v-model="form.overTimeStock" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.modifyNum')" prop="modifyNum">
            <el-input-number v-model="form.modifyNum" :min="-999999" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.badNum')" prop="badNum">
            <el-input-number v-model="form.badNum" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.isEndingSku')" prop="isEndingSku">
            <el-select v-model="form.isEndingSku" filterable clearable :placeholder="$t('common.rule.select')" style="width: 100%">
              <el-option
                v-for="item in dict.type.biz_yes_no"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
            <el-input type="textarea" v-model="form.remark" :rows="3" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">{{ $t('ui.frame.btn.cancel') }}</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">{{ $t('ui.frame.btn.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { saveCxStock } from '@/api/cx/cxStock'

export default {
  name: 'infoDialog',
  inject: ['parentDict'],
  data() {
    const rules = {
      stockDate: [{ required: true, message: this.$t('common.rule.required'), trigger: 'change' }],
      embryoCode: [{ required: true, message: this.$t('common.rule.required'), trigger: 'blur' }],
      stockNum: [{ required: true, message: this.$t('common.rule.required'), trigger: 'blur' }]
    }
    return {
      visible: false,
      loading: false,
      title: '',
      dict: this.parentDict,
      form: {},
      rules
    }
  },
  methods: {
    show(row) {
      if (row) {
        this.title = this.$t('ui.frame.btn.update') + this.$t('ui.data.column.cxStock.modelName')
        this.form = { ...row }
      } else {
        this.title = this.$t('ui.frame.btn.add') + this.$t('ui.data.column.cxStock.modelName')
        this.form = {
          isEndingSku: "0"
        }
      }
      this.visible = true
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        saveCxStock(this.form)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.visible = false
            this.$emit('success')
          })
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>
