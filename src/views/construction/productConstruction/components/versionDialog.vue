<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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

import { numberEmpty } from "@/utils/index";

import infoForm from "@/views/components/infoForm.vue";

import {
  editProductConstruction,
  getVersions,
} from "@/api/cx/productConstruction";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        embryoVersion: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireFabricSap1: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireFabricCode1: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        cordSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        cordSpec: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        insideSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        insideCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sidewallSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sidewallCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beadSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireRingSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        tireRingCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beltSap1: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beltCode1: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beltSap2: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        beltCode2: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        articleCrownSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        articleCrownSpec: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        treadSap: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        treadCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      versions: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.productConstruction.modelName");
    },
    columns() {
      let columns = [
        {
          label: this.$t("ui.data.column.productConstruction.embryoCode"),
          prop: "embryoCode",
          span: 24,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
          span: 24,
          disabled: true,
        },
      ];
      // * 1#胎体布：TIRE_FABRIC1
      if (this.editType === "TIRE_FABRIC1") {
        columns.push(
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabricCode1"
            ),
            prop: "tireFabricCode1",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabric1Version"
            ),
            prop: "tireFabric1Version",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 2#胎体布：TIRE_FABRIC2
      if (this.editType === "TIRE_FABRIC2") {
        columns.push(
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabricCode2"
            ),
            prop: "tireFabricCode2",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabric2Version"
            ),
            prop: "tireFabric2Version",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 3#胎体布：TIRE_FABRIC3
      if (this.editType === "TIRE_FABRIC3") {
        columns.push(
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabricCode3"
            ),
            prop: "tireFabricCode3",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireFabric3Version"
            ),
            prop: "tireFabric3Version",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 帘布大卷：CORD
      if (this.editType === "CORD") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.cordSpec"),
            prop: "cordSpec",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.cordVersion"),
            prop: "cordVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 内衬：INSIDE
      if (this.editType === "INSIDE") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.insideCode"),
            prop: "insideCode",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.insideVersion"),
            prop: "insideVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 胎侧：SIDEWALL
      if (this.editType === "SIDEWALL") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.sidewallCode"),
            prop: "sidewallCode",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.sidewallVersion"
            ),
            prop: "sidewallVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 钢丝圈：BEAD
      if (this.editType === "BEAD") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.beadCode"),
            prop: "beadCode",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.beadVersion"),
            prop: "beadVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 胎圈：TIRE_RING
      if (this.editType === "TIRE_RING") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.tireRingCode"),
            prop: "tireRingCode",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.tireRingVersion"
            ),
            prop: "tireRingVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 1#钢带：BELT1
      if (this.editType === "BELT1") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.beltCode1"),
            prop: "beltCode1",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.belt1Version"),
            prop: "belt1Version",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 2#钢带：BELT2
      if (this.editType === "BELT2") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.beltCode2"),
            prop: "beltCode2",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.belt2Version"),
            prop: "belt2Version",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 钢压大卷：ARTICLE_CROWN
      if (this.editType === "ARTICLE_CROWN") {
        columns.push(
          {
            label: this.$t(
              "ui.data.column.productConstruction.articleCrownSpec"
            ),
            prop: "articleCrownSpec",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t(
              "ui.data.column.productConstruction.articleCrownVersion"
            ),
            prop: "articleCrownVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }
      // * 胎面：TREAD
      if (this.editType === "TREAD") {
        columns.push(
          {
            label: this.$t("ui.data.column.productConstruction.treadCode"),
            prop: "treadCode",
            span: 24,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.productConstruction.treadVersion"),
            prop: "treadVersion",
            span: 24,
            type: "select",
            dictData: this.versions,
          }
        );
      }

      return columns;
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editProductConstruction(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getVersions(id) {
      try {
        this.loading = true;
        const res = await getVersions({
          idAndQueryType: `${id}&${this.editType}`,
        });
        this.versions = res.versions;
        this.form = res.cxProductConstructionInfo;
        this.loading = false;
      } catch (error) {
        this.loading = false;
        console.error(error);
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        this.getVersions(data.id);
      } else {
        //
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
      this.$refs.form.triggerConfirm((params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });
        this.save(params);
      });
    },
    handleSapCodeBlur() {
      if (this.form.sapCode) {
        this.getSpecDesc();
      }
    },
  },
};
</script>
