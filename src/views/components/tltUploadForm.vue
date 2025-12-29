<script setup>
</script>

<template>
  <el-dialog
    :title="title"
    :visible.sync="upload.open"
    width="400px"
    append-to-body
    @close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="upload.submitLoading">
      <info-form
        class="form-item-height"
        ref="form"
        :form="defaultValue"
        :rules="rules"
        :columns="columns"
        label-position="right"
        :label-width="labelWidth"
      />
      <el-upload
        class="form-uploader"
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
         :before-upload="beforeUpload"
        :headers="upload.headers"
        :action="
          upload.url + uploadUrl
        "
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        :on-change="handleChange"
        :data="upload.data"
        drag
      >
        <i class="el-icon-upload" style="margin: 20px 0 16px"></i>
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
        </div>
      </el-upload>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        :disabled="upload.submitLoading || upload.fileList.length == 0"
        @click="confirmUpload"
        >{{ $t("common.button.confirm") }}</el-button
      >
      <el-button @click="upload.open = false">{{
        $t("common.button.cancel")
      }}</el-button>
    </div>
  </el-dialog>
</template>
<script>
import infoForm from "./infoForm.vue";

export default {
  components: { infoForm },
  props: {
    value: String | Number,
    disabled: Boolean,
    downloadUrl: String, //下载模板地址
    downloadUrlFormatter: Function,
    uploadUrl: String, //上传模板地址
    options: Array,
    columns: Array,
    rules: Object,
    // defaultValue: Object,
    labelPosition: { type: String, default: "right" },
    labelWidth: { type: String, default: "80px" },
    title: String,
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
        updateSupport: false,
        // 设置上传的请求头部
        // headers: { Authorization: "Bearer " + getToken() },
        //上传时附带的额外参数
        data:{},
        // 上传的地址
        url: process.env.VUE_APP_BASE_API,
        submitLoading: false,
        //文件列表
        fileList: [],
      },
      defaultValue:{},
    };
  },

  methods: {
     //上传前校验
     beforeUpload(file) {
      // 或者通过文件后缀名校验（更可靠）
      const fileExtension = file.name.split(".").pop().toLowerCase();
      const isExcelByExtension = ["xlsx", "xls"].includes(fileExtension);

      if (!isExcelByExtension) {
        this.$message.error(this.$t("common.upload.onlyXlsXlsx"));
        this.upload.isUploading = false;
        this.upload.submitLoading = false;
        return false; // 阻止上传
      }
    },
    handleTemplateDownload() {
      let downloadDom = document.createElement("a");
      if (this.downloadUrlFormatter) {
        downloadDom.href =
          process.env.VUE_APP_BASE_API +
          this.downloadUrlFormatter(this.$refs.form.getValues());
      } else {
        downloadDom.href = process.env.VUE_APP_BASE_API + this.downloadUrl;
      }

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
    confirmUpload(){
      //将获取输入的表单文件
      this.upload.data = this.$refs.form.getValues();
      console.log(this.upload.data,this.$refs.form.getValues());
      setTimeout(() => {
        this.$refs.form.triggerConfirm(this.submitFileForm);
      }, 0);
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

    handleImport(data) {
      this.updateSupport=false
      if (data) {
        this.defaultValue = {
          ...data,

        };
        // console.log(this.defaultValue);
      }
      this.upload.open = true;
    },
    handleClose() {
      // this.$refs.form.triggerResetForm();

      this.upload.submitLoading = false;
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
::v-deep .form-uploader .el-upload-dragger {
  height: 140px;
}
</style>
