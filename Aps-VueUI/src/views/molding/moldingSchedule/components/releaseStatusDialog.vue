<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form ref="infoForm">
      <el-form-item :label="$t('ui.data.column.scheduleResult.isRelease')">
        <el-radio-group v-model="form.isRelease">
          <el-radio
            v-for="(item, index) in parentDict.type.IS_RELEASE"
            :key="`${item.value}-${index}`"
            :label="item.value"
            >{{ item.label }}</el-radio
          >
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { changeReleaseStatus } from "@/api/cx/cxScheduleResult";

export default {
  components: {},
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.modalName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);

      try {
        this.loading = true;
        const data = await changeReleaseStatus(params);
        this.$modal.msgSuccess(data.msg);

        this.loading = false;
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
