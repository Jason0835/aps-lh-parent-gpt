<script setup>

</script>

<template>
  <el-dialog
    :title="upload.title"
    :visible.sync="upload.open"
    width="400px"
    append-to-body
  >
    <el-upload
      ref="upload"
      :limit="1"
      :headers="upload.headers"
      :action="upload.url+ uploadUrl + '?updateSupport=' + upload.updateSupport"
      :disabled="upload.isUploading"
      :on-progress="handleFileUploadProgress"
      :on-success="handleFileSuccess"
      :auto-upload="false"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">{{$t("common.upload.dragFileText")}}<em>{{$t("common.upload.clickUpload")}}</em></div>
      <div class="el-upload__tip text-center" slot="tip" v-show="downloadUrl">
        <el-link
          type="primary"
          :underline="false"
          style="font-size: 12px; vertical-align: baseline"
          @click="handleTemplateDownload"
        >{{$t("common.upload.downloadTemplate")}}</el-link
        >
      </div>
    </el-upload>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitFileForm">{{$t("common.button.confirm")}}</el-button>
      <el-button @click="upload.open = false">{{$t("common.button.cancel")}}</el-button>
    </div>
  </el-dialog>
</template>
<script>

export default {
  props: {
    value: String | Number,
    disabled: Boolean,
    downloadUrl: String,//下载模板地址
    uploadUrl: String,//上传模板地址
    options: Array,
  },
  data() {
    return {
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        // headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url:
          process.env.VUE_APP_BASE_API,
      },
    };
  },

  methods: {
    handleTemplateDownload() {
      let downloadDom = document.createElement("a");
      downloadDom.href =
        process.env.VUE_APP_BASE_API +
        this.downloadUrl;
      document.body.appendChild(downloadDom);
      downloadDom.click();
      document.body.removeChild(downloadDom);
    },

    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
        response.msg +
        "</div>",
        this.$t("common.upload.importResult"),
        { dangerouslyUseHTMLString: true }
      );
      this.$emit("uploadSuccess");
      // this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },

    handleImport() {
      this.upload.open = true;
    },


  },
  mounted() {
    //创建时间赋值给上传地址
  },
};
</script>
<style scoped lang="scss">

</style>
