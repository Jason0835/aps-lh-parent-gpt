<!--
 * @Description: 文档-左侧表格 页面
 * @Author: qy
 * @Date: 2024/2/20
-->
<template>
  <div>
    <el-button
      class="mb-10"
      type="primary"
      size="mini"
      v-if="showRightUploadBtn"
      @click="handleUpload"
      ><svg-icon icon-class="upload" class="svg-icon cursor-pointer" />
      {{$t("common.button.upload")}}</el-button
    >
    <el-table :data="data" border size="mini">
      <el-table-column type="selection" width="40"> </el-table-column>
      <el-table-column prop="fileName" :label="$t('common.annexName')"> </el-table-column>
      <el-table-column :label="$t('common.option')" align="center" width="100">
        <template slot-scope="scope">
          <svg-icon
            class="cursor-pointer svg-icon"
            icon-class="eye-open"
            style="margin-right: 5px"
            @click="handleDownload(scope.row)"
          />
          <svg-icon
            class="cursor-pointer svg-icon"
            icon-class="download"
            style="margin-right: 5px"
            @click="handleDownload(scope.row)"
          />
          <svg-icon
            class="cursor-pointer svg-icon"
            icon-class="del"
            @click="handleDelete(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createByName" :label="$t('common.uploadBy')" width="180">
      </el-table-column>
      <el-table-column prop="updateTime" :label="$t('common.uploadTime')" width="180">
      </el-table-column>
      <el-table-column prop="uploadNum" :label="$t('common.uploadNum')" width="90">
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { deleteFileSync } from "@/api/bd/billFile";
export default {
  props: {
    parentInfo: {
      type: Object,
      default: () => {
        return null;
      },
    },
    showRightUploadBtn: {
      type: Boolean,
      default: false,
    },
    data: {
      type: Array,
      default: () => {
        return [];
      },
    },
  },
  data() {
    return {};
  },
  watch: {
    parentInfo() {
      this.getList();
    },
  },
  mounted() {
    this.getList();
  },
  methods: {
    getList() {
      if (!this.parentInfo) {
        return false;
      }
    },
    async handleDownload(row) {
      window.location.href = `${process.env.VUE_APP_BASE_API}/common/fileUpload/downloadFile/${row.bizId}/${row.fileUniKey}/${row.fileName}`;
    },
    handleUpload() {
      this.$emit("openFolder");
    },
    handleDelete(row) {
      const that = this;
      if (row.fileUniKey) {
        that
          .$confirm(this.$t("common.confirm.delete"), {
            type: "warning",
          })
          .then(async () => {
            deleteFileSync({
              key: row.fileUniKey,
            })
              .then((res) => {
                that.$modal.msgSuccess(this.$t("common.msg.success.delete"));
                that.$emit("refresh");
                that.$emit("delete", row);
              })
              .finally(() => {
                that.$emit("closeLoading");
              });
          })
          .catch();
      }
    },
  },
};
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
.svg-icon {
  font-size: 18px !important;
}
</style>
