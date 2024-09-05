<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import { lhValidateAutoPlan, autoPlan } from "@/api/lh/scheduleResult";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      defaultValue: {},
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.lhAutoPlan");
    },
  },
  methods: {
    // api
    async handleAutoPlan(params) {
      // console.log(params);
      try {
        this.loading = true;
        const result = await lhValidateAutoPlan(params);
        if (result.msg == "1") {
          //已经生成，提示是否重新生成
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate")).then(
            async () => {
              try {
                const data = await autoPlan(params);
                this.$modal.msgSuccess(data.msg);
                this.$emit("success", params);
              } catch (error) {
                console.error(error);
              }
            }
          );
        } else if (result.msg == "2") {
          //未生成，直接生成
          const data = await autoPlan(params);
          this.$modal.msgSuccess(data.msg);
          this.$emit("success", params);
        } else if (result.msg == "3") {
          //已发布，提示不能重新生成
          this.$modal.msgError(this.$t("ui.biz.alter.CanNotRecreate"));
        }

        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      //设置默认值为明天
      let nowDate = new Date();
      let tomorrow = new Date(nowDate);
      tomorrow.setDate(nowDate.getDate() + 1);
      this.defaultValue = {
        scheduleDate: tomorrow.toISOString().slice(0, 10), // 保留yyyy-MM-dd
      };
      // if (data) {
      //   this.isEdit = true;
      //   this.defaultValue = {
      //     ...data,
      //   };
      // }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.handleAutoPlan);
    },
  },
};
</script>
