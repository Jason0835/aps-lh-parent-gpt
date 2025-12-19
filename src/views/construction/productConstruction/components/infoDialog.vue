<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1100px"
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
  edit1ProductConstruction,
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
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.productConstruction.modelName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.construction.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoCode"),
          prop: "sapCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.construction.dimension"),
          prop: "dimension",
          span: 12,
          type: "number",
          min: 0,
          max: 99999999,
        },
        {
          label: this.$t("ui.construction.spec"),
          prop: "spec",
          span: 12,
          maxlength: "200",
        },
        {
          label: this.$t("ui.construction.noseWidth"),
          prop: "noseWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 99999999,
        },
        {
          label: this.$t("ui.construction.flipDiscDiameter"),
          prop: "flipDiscDiameter",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.fitDrumPerimeter"),
          prop: "fitDrumPerimeter",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.chuckDiameter"),
          prop: "chuckDiameter",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.stretchWidth"),
          prop: "stretchWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.qualitativeWidth"),
          prop: "qualitativeWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.embryoCircle"),
          prop: "embryoCircle",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
        },
        {
          label: this.$t("ui.construction.sectionWidth"),
          prop: "sectionWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 999999999,
          precision: 0,
        },

        {
          label: this.$t("ui.construction.carcassCloth"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricSap1"),
          prop: "tireFabricSap1",
          span: 12,
          required: true,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricCode1"),
          prop: "tireFabricCode1",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.tireFabricCraft1"),
          prop: "tireFabricCraft1",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t(
            "ui.data.column.productConstruction.tireFabricSideRubber"
          ),
          prop: "tireFabricSideRubber",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricSap2"),
          prop: "tireFabricSap2",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricCode2"),
          prop: "tireFabricCode2",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.tireFabricCraft2"),
          prop: "tireFabricCraft2",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricSap3"),
          prop: "tireFabricSap3",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.data.column.productConstruction.tireFabricCode3"),
          prop: "tireFabricCode3",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.tireFabricCraft3"),
          prop: "tireFabricCraft3",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.cordSap"),
          prop: "cordSap",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.cordSpec"),
          prop: "cordSpec",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.reinforceSealGlue"),
          prop: "reinforceSealGlue",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.lining"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.insideSap"),
          prop: "insideSap",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.insideCode"),
          prop: "insideCode",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.insideRubber"),
          prop: "insideRubber",
          span: 12,
          maxlength: "20",
          // required: true
        },
        {
          label: this.$t("ui.construction.insideCraft"),
          prop: "insideCraft",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.sidewall"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.sidewallSap"),
          prop: "sidewallSap",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.sidewallCode"),
          prop: "sidewallCode",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.sidewallCraft"),
          prop: "sidewallCraft",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.sidewallMouthPlate"),
          prop: "sidewallMouthPlate",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.sidewallCenter"),
          prop: "sidewallCenter",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.sidewallLength"),
          prop: "sidewallLength",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.sidewallRubber"),
          prop: "sidewallRubber",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.sidewallWeight"),
          prop: "sidewallWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.sidewallWearpRubberWeight"),
          prop: "sidewallWearpRubberWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.supportingGlue"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.supportCode"),
          prop: "supportCode",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.supportRubberCode"),
          prop: "supportRubberCode",
          span: 12,
          maxlength: "20",
        },
        {
          label: this.$t("ui.construction.supportLength"),
          prop: "supportLength",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.steelRing"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.beadSap"),
          prop: "beadSap",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beadCode"),
          prop: "beadCode",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beadArrange"),
          prop: "beadArrange",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.beadType"),
          prop: "beadType",
          span: 12,
          maxlength: "16",
          required: false,
        },
        {
          label: this.$t("ui.construction.hexagonRing"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.tireRingSap"),
          prop: "tireRingSap",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.tireRingCode"),
          prop: "tireRingCode",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.apexCode"),
          prop: "apexCode",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.hexagonRubberCode"),
          prop: "hexagonRubberCode",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.hexagonMouthPlate"),
          prop: "hexagonMouthPlate",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.hexagonRubberDimension"),
          prop: "hexagonRubberDimension",
          span: 12,
          maxlength: "16",
          required: false,
        },
        {
          label: this.$t("ui.construction.apexWeight"),
          prop: "apexWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.steelStrip"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.beltSap1"),
          prop: "beltSap1",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beltCode1"),
          prop: "beltCode1",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beltCraft1"),
          prop: "beltCraft1",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.beltSideRubber1"),
          prop: "beltSideRubber1",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.beltRubber1"),
          prop: "beltRubber1",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.beltSap2"),
          prop: "beltSap2",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beltCode2"),
          prop: "beltCode2",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.beltCraft2"),
          prop: "beltCraft2",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.beltSideRubber2"),
          prop: "beltSideRubber2",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.beltRubber2"),
          prop: "beltRubber2",
          span: 12,
          maxlength: "10",
          required: false,
        },
        {
          label: this.$t("ui.construction.beltCuttingAngle"),
          prop: "beltCuttingAngle",
          span: 12,
          maxlength: "10",
          required: false,
        },
        {
          label: this.$t("ui.construction.articleCrownSap"),
          prop: "articleCrownSap",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.articleCrownSpec"),
          prop: "articleCrownSpec",
          span: 12,
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.construction.crownBand"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.articleCrownCode"),
          prop: "articleCrownCode",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.tread"),
          type: "title",
        },
        {
          label: this.$t("ui.construction.treadSap"),
          prop: "treadSap",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.treadCode"),
          prop: "treadCode",
          span: 12,
          maxlength: "20",
          required: false,
        },
        {
          label: this.$t("ui.construction.treadShoulderWidth"),
          prop: "treadShoulderWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.treadShoulderJWidth"),
          prop: "treadShoulderJWidth",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.treadShoulderLength"),
          prop: "treadShoulderLength",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.treadRubberCategory"),
          prop: "treadSap",
          span: 12,
          maxlength: "50",
          required: false,
        },
        {
          label: this.$t("ui.construction.tireCrownUpWidthWeight"),
          prop: "tireCrownUpWidthWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.tireCrownDownWidthWeight"),
          prop: "tireCrownDownWidthWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.tireWingWidthWeight"),
          prop: "tireWingWidthWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.primerWeight"),
          prop: "primerWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.conductingResinWeight"),
          prop: "conductingResinWeight",
          span: 12,
          type: "number",
          min: 0,
          max: 99999,
          // precision: 0,
        },
        {
          label: this.$t("ui.construction.treadMouthPlate"),
          prop: "treadMouthPlate",
          span: 12,
          maxlength: "20",
          required: false,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await edit1ProductConstruction(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getSpecDesc() {
      try {
        const res = await getSpecDesc({
          sapCode: this.form.sapCode,
        });
        if (res && res.specDesc) {
          this.$set(this.form, "specDesc", res.specDesc);
        } else {
          this.$set(this.form, "specDesc", "");

          this.$alert(res.msg, {
            type: "error",
          });
        }
      } catch (error) {
        console.error(error);
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
          dimension: numberEmpty(data.dimension),
          noseWidth: numberEmpty(data.noseWidth),
          flipDiscDiameter: numberEmpty(data.flipDiscDiameter),
          fitDrumPerimeter: numberEmpty(data.fitDrumPerimeter),
          chuckDiameter: numberEmpty(data.chuckDiameter),
          stretchWidth: numberEmpty(data.stretchWidth),
          qualitativeWidth: numberEmpty(data.qualitativeWidth),
          embryoCircle: numberEmpty(data.embryoCircle),
          sectionWidth: numberEmpty(data.sectionWidth),
          tireFabricCraft1: numberEmpty(data.tireFabricCraft1),
          tireFabricCraft2: numberEmpty(data.tireFabricCraft2),
          tireFabricCraft3: numberEmpty(data.tireFabricCraft3),
          insideCraft: numberEmpty(data.insideCraft),
          sidewallCraft: numberEmpty(data.sidewallCraft),
          sidewallLength: numberEmpty(data.sidewallLength),
          sidewallWeight: numberEmpty(data.sidewallWeight),
          sidewallWearpRubberWeight: numberEmpty(
            data.sidewallWearpRubberWeight
          ),
          apexWeight: numberEmpty(data.apexWeight),
          beltCraft1: numberEmpty(data.beltCraft1),
          beltCraft2: numberEmpty(data.beltCraft2),
          treadShoulderWidth: numberEmpty(data.treadShoulderWidth),
          treadShoulderJWidth: numberEmpty(data.treadShoulderJWidth),
          treadShoulderLength: numberEmpty(data.treadShoulderLength),
          tireCrownUpWidthWeight: numberEmpty(data.tireCrownUpWidthWeight),
          tireCrownDownWidthWeight: numberEmpty(data.tireCrownDownWidthWeight),
          tireWingWidthWeight: numberEmpty(data.tireWingWidthWeight),
          primerWeight: numberEmpty(data.primerWeight),
          conductingResinWeight: numberEmpty(data.conductingResinWeight),
          supportLength: numberEmpty(data.supportLength),
        };
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
