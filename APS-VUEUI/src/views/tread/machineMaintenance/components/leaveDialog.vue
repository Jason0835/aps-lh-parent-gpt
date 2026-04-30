<template>
  <el-dialog
    title="操作工请假"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
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
import moment from "moment";
import { mapState } from "vuex";

import infoForm from "@/views/components/infoForm.vue";

import { validateAdd, editScheduleResult } from "@/api/cd15/scheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        机台: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        操作工: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        请假班次: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.cut15.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.cd15ScheduleResult.modalName");
    },
    columns() {
      return [


        {
          label: this.$t("机台"),
          prop: "机台",
          span: 24,
          type: "select",
        },
        {
          label: this.$t("操作工"),
          prop: "操作工",
          span: 24,
        },
        {
          label: this.$t("请假班次"),
          prop: "请假班次",
          type: "select",
        },


      ];
    },
  },
  methods: {
    // api
    validateAdd(params) {
      return new Promise(async (resolve, reject) => {
        try {
          let valid = await validateAdd(params);
          if (valid.msg == "0") {
            this.$confirm(
              this.$t("ui.data.column.scheduleResult.isContinueAdd")
            )
              .then(async () => {
                resolve();
              })
              .catch((error) => {
                reject(error);
              });
          } else {
            resolve();
          }
        } catch (error) {
          reject(error);
        }
      });
    },

    async save(params) {
      // try {
      //   this.loading = true;
      //   await this.validateAdd(params);
      //   let result = await editScheduleResult(params);
      //   this.loading = false;
      //   if (result.code == 200) {
      //     this.$modal.msgSuccess(result.msg);
      //     this.$emit("success");
      //     this.hide();
      //   }
      // } catch (error) {
      //   console.error(error);
      //   this.loading = false;
      // }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
        }
      }
    },
    hide() {
      // this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
