<script setup>
</script>

<template>
  <el-dialog
    :title="upload.title"
    :visible.sync="upload.open"
    width="400px"
    append-to-body
    @close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-upload
      ref="upload"
      :limit="1"
      :on-exceed="handleExceed"
      accept=".xls,.xlsx"
      :headers="upload.headers"
      :action="upload.url + uploadUrl + '?updateSupport=' + updateSupport"
      :disabled="upload.isUploading"
      :on-progress="handleFileUploadProgress"
      :on-success="handleFileSuccess"
      :on-error="handleFileError"
      :auto-upload="false"
      v-loading="upload.submitLoading"
      :on-change="handleChange"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">
        {{ $t("common.upload.dragFileText") }}
        <em>{{ $t("common.upload.clickUpload") }}</em>
      </div>
      <div class="el-upload__tip text-center" slot="tip">
        <span>{{ $t("common.upload.onlyXlsXlsx") }}</span>
        <el-link
          v-if="downloadUrl"
          type="primary"
          :underline="false"
          style="font-size: 12px; vertical-align: baseline"
          @click="handleTemplateDownload"
          >{{ $t("common.upload.downloadTemplate") }}</el-link
        >
        <div><slot name="tip"> </slot></div>
      </div>
    </el-upload>
    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        :disabled="upload.submitLoading || upload.fileList.length == 0"
        @click="submitFileForm"
        >{{ $t("common.button.confirm") }}</el-button
      >
      <el-button @click="upload.open = false">{{
        $t("common.button.cancel")
      }}</el-button>
    </div>
  </el-dialog>
</template>
<script>
export default {
  props: {
    value: String | Number,
    disabled: Boolean,
    downloadUrl: String, //下载模板地址
    uploadUrl: String, //上传模板地址
    options: Array,
    updateSupport: {
      // 是否更新已经存在的用户数据
      type: Number | Boolean,
      default: 0,
    },
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
        // updateSupport: 0,
        // 设置上传的请求头部
        // headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API,
        submitLoading: false,
        //文件列表
        fileList: [],
      },
    };
  },

  methods: {
      // 超过限制时的处理
      handleExceed(files, fileList) {
        // this.$message.warning('正在替换文件...')

      // 清空当前文件
      this.fileList = []

      // 使用 $nextTick 确保UI更新
      this.$nextTick(() => {
        const uploadRef = this.$refs.upload
        if (uploadRef) {
          // 清空组件内部状态
          uploadRef.clearFiles()

          // 触发新文件上传
          uploadRef.handleStart(files[0])
        }
      })
    },
    handleTemplateDownload() {
      let downloadDom = document.createElement("a");
      downloadDom.href = process.env.VUE_APP_BASE_API + this.downloadUrl;
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
      this.upload.submitLoading = false;
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
    /**文件上传失败 */
    handleFileError() {
      this.upload.submitLoading = false;
    },
    // 提交上传文件
    submitFileForm() {
      try {
        this.upload.submitLoading = true;
        this.$refs.upload.submit();
      } catch (error) {
        this.upload.submitLoading = false;
      }
    },

    handleImport() {
      this.upload.open = true;
    },
    handleClose() {
      this.upload.submitLoading = false;
      this.upload.fileList = [];
      this.$refs.upload.clearFiles();
    },
    handleChange(file, fileList) {
      this.upload.fileList = fileList;
      // console.log(this.upload.fileList); // 这里可以获取到当前的文件列表
    },
  },
  mounted() {
    //创建时间赋值给上传地址
  },
};
</script>
<style scoped lang="scss">
</style>
