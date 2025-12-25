<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div
      style="
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      "
    >
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="columns"
        label-position="right"
        label-width="100px"
        v-loading="loading"
      >
      </info-form>
      <div style="width: 100px; text-align: center">{{ this.$t('ui.text.monthlyproductiontarget.copyto') }}</div>
      <info-form
        class="form-item-height"
        ref="copyForm"
        :form="copyForm"
        :rules="rules"
        :columns="copyColumns"
        label-position="right"
        label-width="100px"
        v-loading="loading"
      >
      </info-form>
    </div>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";

import {
  saveAreaCapaInfo,
  copyAreaCapaInfo,
  copyCheckAreaCapaInfo,
} from "@/api/monthplan/mdmAreaCapaAllocation";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      copyForm: {},
      rules: {
        sourceFactoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        targetFactoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        targeYearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],

        // productCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // locationType: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.params.copy")
    },
    columns() {
      return [
        {
          prop: "sourceFactoryCode",
          label: this.$t("ui.data.column.regionalCapacityAllocation.sourceFactoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },
    copyColumns() {
      return [
        {
          prop: "targetFactoryCode",
          label: this.$t("ui.data.column.regionalCapacityAllocation.targetFactoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "targeYearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },
  },
  methods: {
    // api
    async checkCopy(params) {
      try {
        this.loading = true;

        const res = await copyCheckAreaCapaInfo(params);
        console.log("checkCopy res", res);
        this.$confirm(res.msg, {
          type: "warning",
        }).then(() => {
          copyAreaCapaInfo(params).then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$emit("success");
            this.hide();
          });
        });
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show() {
      this.visible = true;
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        this.$refs.copyForm.triggerConfirm(async (copyParams) => {
          let array = params.yearMonth.split("-");
          params.sourceYear = array[0];
          params.sourceMonth = array[1];
          let arrayCopy = copyParams.targeYearMonth.split("-");
          params.targetYear = array[0];
          params.targetMonth = array[1];
          let obj = {
            ...params,
            ...copyParams,
          };
          console.log("obj", obj);
          try {
            this.loading = true;
            this.checkCopy(obj);
          } catch (error) {
            console.error(error);
            this.$modal.msgError(error.message);
            this.loading = false;
          }
        });
      });
    },
  },
};
</script>
