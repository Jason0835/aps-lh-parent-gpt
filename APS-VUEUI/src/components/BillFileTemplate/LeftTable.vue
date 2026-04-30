<!--
 * @Description: 文档-左侧表格 页面
 * @Author: qy
 * @Date: 2024/2/20
-->
<template>
  <div>
    <el-table
      :data="data"
      border
      size="mini"
      highlight-current-row
      @current-change="handleCurrentChange"
    >
      <el-table-column
        type="selection"
        width="40"
      />
      <el-table-column
        prop="tempFileType"
        :label="$t('common.fileType')"
      />
      <el-table-column
        prop="status"
        :label="$t('common.status')"
      >
        <template slot-scope="scope">{{ scope.row.status + '' === '1' ? $t('common.enable') : scope.row.status + '' === '2' ? $t('common.disable') : scope.row.status }} </template>
      </el-table-column>
      <el-table-column
        prop="address"
        :label="$t('common.option')"
        align="center"
        width="90"
      >
        <template slot-scope="scope">
          <svg-icon icon-class="upload" class="svg-icon cursor-pointer" v-show="scope.row.canUpload === '1'" style="margin-right: 5px" @click="handleOpenLib(scope.row)" />
          <svg-icon icon-class="print" class="svg-icon cursor-pointer"  @click="handlePrint(scope.row)" />
        </template>
      </el-table-column>
    </el-table>
    <PrintDialog :visible="printDialog.show" :render-form="renderPrintDialogForm" @close="printDialog.show = false" @submit="handleSubmit" />
  </div>
</template>

<script>
import PrintDialog from './printDialog'
import { baseRequest } from '@/api/bd/billFile'
export default {
  components: {
    PrintDialog
  },
  props: {
    businessId: {
      type: String
    },
    bizId: {
      type: String
    },
    billId: {
      type: String
    },
    data: {
      type: Array,
      default: () => {
        return []
      }

    },
    renderPrintDialogForm: {
      type: Function,
      default: null
    }
  },
  data() {
    return {
      printDialog: {
        show: false
      },
      printRow: {},
      loading: false,
      selectRow: {}
    }
  },
  mounted() {
  },
  methods: {
    handleCurrentChange(row) {
      this.$emit('change', row)
    },
    async handleSubmit(form) {
      const that = this
      this.$emit('showLoading')
      baseRequest(this.printRow.itfUrl, 'post', { ...form, id: this.billId }).then(res => {
        that.$emit('refresh')
      }).finally(() => {
        that.$emit('closeLoading')
      })
    },
    handleOpenLib(row) {
      // this.selectRow = row
      // this.$refs.uploadInput.click()
      this.$emit('openFolder', row)
    },
    handlePrint(row) {
      this.printRow = row
      if (!row.itfUrl || !row.id) {
        return
      }
      if (this.renderPrintDialogForm) {
        this.printDialog.show = true
      } else {
        this.handleSubmit({})
      }
    }
  }
}
</script>

<style scoped>
.cursor-pointer{
  cursor: pointer;
}
.svg-icon {
  font-size: 18px!important;
}
</style>
