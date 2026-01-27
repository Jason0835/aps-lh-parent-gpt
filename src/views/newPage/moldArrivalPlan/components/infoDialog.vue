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
import { mapState } from "vuex";

import { editMpMouldShellInfo ,getBoardingDate} from "@/api/monthplan/mpMouldDeliveryPlan";

import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import infoForm from "@/views/components/infoForm.vue";
import { max } from "lodash";

export default {
  components: { infoForm,materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        shipmentDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        materialDesc: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        mainPattern: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
          maxlength: 32,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          maxlength: 32,
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          maxlength: 64,
          disabled: true,
        },
        {
          prop: "shipmentDate",
          label: this.$t("ui.data.column.monthplan.shipmentDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.shipmentDateChange,
          },
        },
        {
          prop: "boardingDate",
          label: this.$t("ui.data.column.monthplan.boardingDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          maxlength:60
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength: 500,
        },
      ];
    },
  },
  methods: {
  async shipmentDateChange(){
      if(this.form.shipmentDate){
        try {
          const res = await getBoardingDate({shipmentDate:this.form.shipmentDate});

          this.$set(this.form, 'boardingDate', res.msg)
        } catch (error) {
          console.log(error);
        }
      }
    },
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMpMouldShellInfo(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
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
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
      } else {
        this.$set(this.form, "materialDesc", "");
      }
    },
  },
};
</script>
