<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.factoryCode')" prop="factoryCode">
            <el-select
              v-model="form.factoryCode"
              filterable
              clearable
              :placeholder="$t('common.rule.select')"
              style="width: 100%"
            >
              <el-option
                v-for="item in dict.type.biz_factory_name"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
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
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.embryoCode')" prop="embryoCode">
            <el-input v-model="form.embryoCode" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.stockNum')" prop="stockNum">
            <el-input-number v-model="form.stockNum" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.overTimeStock')" prop="overTimeStock">
            <el-input-number v-model="form.overTimeStock" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.modifyNum')" prop="modifyNum">
            <el-input-number v-model="form.modifyNum" :min="-999999" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.badNum')" prop="badNum">
            <el-input-number v-model="form.badNum" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxStock.isEndingSku')" prop="isEndingSku">
            <el-select
              v-model="form.isEndingSku"
              filterable
              clearable
              :placeholder="$t('common.rule.select')"
              style="width: 100%"
            >
              <el-option
                v-for="item in dict.type.biz_yes_no"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
            <el-input type="textarea" v-model="form.remark" :rows="3" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t('common.button.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { saveCxStock } from '@/api/cx/cxStock'

export default {
  name: 'InfoDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      isEdit: false,
      dict: this.parentDict,
      form: {},
      rules: {
        stockDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        embryoCode: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }],
        stockNum: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    title() {
      return this.$t('ui.data.column.cxStock.modelName')
    }
  },
  methods: {
    show(row) {
      this.visible = true
      if (row) {
        this.isEdit = true
        this.form = { ...row }
      } else {
        this.isEdit = false
        this.form = {
          isEndingSku: '0'
        }
      }
    },
    hide() {
      this.visible = false
      this.form = {}
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    handleConfirm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        saveCxStock(this.form)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.$emit('success')
            this.hide()
          })
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-form-item {
  margin-bottom: 25px;
}
</style>
