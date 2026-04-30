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
    <el-form ref="infoForm" :model="form">
      <el-form-item :label="$t('ui.data.column.scheduleResult.isRelease')">
        <el-radio-group v-model="form.isRelease">
          <template v-for="(item, index) in parentDict.type.IS_RELEASE">
            <el-radio
              v-if="item.value == '1' || item.value == '2'"
              :key="`${item.value}-${index}`"
              :label="item.value"
              >{{ item.label }}</el-radio
            ></template
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
import { changeReleaseStatus } from "@/api/tm/tmScheduleResult";

export default {
  components: {},
  props: {
    scheduleDate: {
      type: String,
      require: true,
    },
  },
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
      return this.$t("ui.data.column.tmScheduleResult.modalName");
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
      this.form = {};
      this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.infoForm.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
            scheduleDate: this.scheduleDate
          });
        }
      });
    },
  },
};
</script>
