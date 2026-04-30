<!--
 * @Description: 文档 页面
 * @Author: qy
 * @Date: 2024/2/20
-->
<template>
  <div class="flex w100" style="align-items: start" v-loading="loading">
    <LeftTable
      v-if="showLeft"
      class="left"
      @change="leftChange"
      :data="leftData"
      :billId="billId"
      :billType="billType"
      :bizId="bizId"
      :renderPrintDialogForm="renderPrintDialogForm"
      @refresh="getList()"
      @showLoading="showLoading"
      @closeLoading="closeLoading"
      @openFolder="handleOpenFolder"
    ></LeftTable>
    <RightTable
      class="flex-1 right"
      :parentInfo="leftCurrent"
      :data="rightData"
      @refresh="getList()"
      @delete="handleDelete"
      @showLoading="showLoading"
      @closeLoading="closeLoading"
      @openFolder="handleOpenFolder"
      :showRightUploadBtn="showRightUploadBtn"
    ></RightTable>
    <input
      ref="uploadInput"
      type="file"
      style="display: none"
      @change="handleUpload"
    />
  </div>
</template>

<script>
import LeftTable from "./LeftTable";
import RightTable from "./RightTable";
import { selectTempAndBillFile, uploadFileSync } from "@/api/bd/billFile";

export default {
  props: {
    showLeft: {
      type: Boolean,
      default: true,
    },
    showRightUploadBtn: {
      type: Boolean,
      default: false,
    },
    // 单据类型  01出运单,送货单02,03对账单, 00 OA审批
    billType: {
      type: String,
      default: "01",
    },
    // 业务ID
    businessId: {
      type: String,
    },
    bizId: {
      type: String,
    },
    // 单据ID
    billId: {
      type: String,
      // default: '1'
    },
    renderPrintDialogForm: {
      type: Function,
      default: null,
    },
    invisibleUrl:{
      type: Array,
      default: () => {
        return []
      }
    },
  },
  components: {
    LeftTable,
    RightTable,
  },
  data() {
    return {
      leftCurrent: null,
      leftData: [],
      rightData: [],
      loading: false,
    };
  },
  mounted() {
    // this.getList()
  },
  watch: {
    bizId: function (val, oldVal) {
      this.getList();
    },
  },
  methods: {
    leftChange(row) {
      this.leftCurrent = row;
    },
    showLoading() {
      this.loading = true;
    },
    closeLoading() {
      this.loading = false;
    },
    async getList() {
      const res = await selectTempAndBillFile({
        billType: this.billType,
        billId: this.billId,
        bizId: this.bizId,
      });
      this.leftData = res.billFileType ? res.billFileType.filter(element => !this.invisibleUrl.includes(element.itfUrl)):res.billFileType;
      this.rightData = res.billFileList;
    },
    handleOpenFolder(row) {
      this.$refs.uploadInput.click();
    },
    handleUpload(e) {
      const that = this;
      if (!e.target.files[0] || !this.bizId) {
        return;
      }
      this.$emit("showLoading");
      uploadFileSync(
        {
          file: e.target.files[0],
          bucket: "",
          dir: "",
          bizId: this.bizId,
        },
        () => {},
        (p) => {
          console.log(p);
        }
      )
        .then((res) => {
          that.$modal.msgSuccess(this.$t("common.msg.success.upload"));
          that.getList();
        })
        .finally(() => {
          this.$refs.uploadInput.value = "";
          that.closeLoading();
        });
    },
    handleDelete(row) {
      this.$emit("delete", row);
    },
  },
};
</script>

<style scoped>
.h100 {
  height: 100%;
}
.flex-1 {
  flex: 1;
}
.left {
  width: 30%;
  flex-shrink: 0;
  margin-right: 15px;
}
.right {
  min-width: 0;
}
</style>
