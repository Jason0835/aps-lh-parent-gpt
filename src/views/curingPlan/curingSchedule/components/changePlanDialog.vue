<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1100px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
    :center="false"
  >
    <div class="content" v-loading="loading">
      <el-form
        ref="form"
        label-position="right"
        label-width="120px"
        :model="form"
      >

        <el-row type="flex" style="flex-wrap: wrap">
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("ui.data.column.scheduleResult.baseInfo") }}
            </h4>
          </el-col>


          <el-col :span="12">
            <el-form-item
              :label="$t('硫化机台')"
              prop="lhMachineCode"
            >
              <el-input v-model="form.lhMachineCode" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('物料编码')"
              prop="materialCode"
            >
              <el-input v-model="form.materialCode" disabled></el-input>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.maximumClassQty')"
                  prop="maximumClassQty"
                >
                  <el-input v-model="form.maximumClassQty" disabled></el-input>
                </el-form-item>
              </el-col> -->
          <!-- <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.minimumLhMachineReqQty')
                  "
                  prop="minimumLhMachineReqQty"
                >
                  <el-input
                    v-model="form.minimumLhMachineReqQty"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col> -->
          <el-col :span="12">
            <el-form-item
              :label="$t('物料描述')"
              prop="materialDesc"
            >
            <el-input v-model="form.materialDesc" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('胎胚描述')"
              prop="mainMaterialDesc"
            >
              <el-input v-model="form.mainMaterialDesc" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('合计余量')"
              prop="mouldSurplusQty"
            >
              <el-input v-model="form.mouldSurplusQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('胎胚库存')"
              prop="embryoStock"
            >
              <el-input v-model="form.embryoStock" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('硫化班产')"
              prop="mouldSurplusQty"
            >
              <el-input v-model="form.mouldSurplusQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.sapCode')"
              prop="sapCode"
            >
              <el-input v-model="form.sapCode" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.br.dayPlanQty.br')"
              prop="class3PlannedQty"
            >
              <el-input v-model="form.class3PlannedQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.singleShiftLhQty')"
              prop="singleShiftLhQty"
            >
              <el-input v-model="form.singleShiftLhQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.cxMonthFinishQty')"
              prop="cxMonthFinishQty"
            >
              <el-input v-model="form.cxMonthFinishQty" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.totalStock')"
              prop="totalStock"
            >
              <el-input v-model="form.totalStock" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.isRelease')"
              prop="isRelease"
            >
              <dict-select
                v-model="form.storageLocation"
                :options="parentDict.type.IS_RELEASE"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.specDimension')"
              prop="specDimension"
            >
              <el-input v-model="form.specDimension" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <el-col :span="24">
            <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
              <el-input
                type="textarea"
                v-model="form.remark"
                disabled
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("早班") +dateList[0].shiftDate }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class1PlanQty"
            >
              <el-input
                v-model="form.class1PlanQty"
                :disabled="three1PlanTimeDisabled"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class1FinishQty"
            >
              <el-input v-model="form.class1FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class1AnalysisInput"
            >
              <el-input
                v-model="form.class1AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class1Analysis"
            >
              <el-input v-model="form.class1Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("中班")+dateList[1].shiftDate }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class2PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class2PlanQty"
                :disabled="three2PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class2FinishQty"
            >
              <el-input v-model="form.class2FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class2AnalysisInput"
            >
              <el-input
                v-model="form.class2AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class2Analysis"
            >
              <el-input v-model="form.class2Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("晚班")+dateList[2].shiftDate }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class3PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class3PlanQty"
                :disabled="three3PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class2FinishQty"
            >
              <el-input v-model="form.class3FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class3AnalysisInput"
            >
              <el-input
                v-model="form.class3AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class3Analysis"
            >
              <el-input v-model="form.class3Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("早") +dateList[3].shiftDate }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class4PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class4PlanQty"
                :disabled="three4PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class4FinishQty"
            >
              <el-input v-model="form.class4FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class4AnalysisInput"
            >
              <el-input
                v-model="form.class4AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class4Analysis"
            >
              <el-input v-model="form.class4Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("中班"+dateList[4].shiftDate) }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class5PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class5PlanQty"
                :disabled="three5PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class5FinishQty"
            >
              <el-input v-model="form.class5FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class5AnalysisInput"
            >
              <el-input
                v-model="form.class5AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class5Analysis"
            >
              <el-input v-model="form.class5Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("晚班"+dateList[5].shiftDate) }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class6PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class6PlanQty"
                :disabled="three6PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class6FinishQty"
            >
              <el-input v-model="form.class6FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class6AnalysisInput"
            >
              <el-input
                v-model="form.class6AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class6Analysis"
            >
              <el-input v-model="form.class6Analysis" disabled></el-input>
            </el-form-item>
          </el-col>


          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("早班"+dateList[6].shiftDate) }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class7PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class7PlanQty"
                :disabled="three7PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class7FinishQty"
            >
              <el-input v-model="form.class7FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class7AnalysisInput"
            >
              <el-input
                v-model="form.class7AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class7Analysis"
            >
              <el-input v-model="form.class7Analysis" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <h4 class="form-header h4">
              {{ $t("中班"+dateList[7].shiftDate) }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class8PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class8PlanQty"
                :disabled="three8PlanTimeDisabled"
                :min="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class8FinishQty"
            >
              <el-input v-model="form.class8FinishQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analysis')"
              prop="class8AnalysisInput"
            >
              <el-input
                v-model="form.class8AnalysisInput"
                maxlength="66"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.analySystem')"
              prop="class8Analysis"
            >
              <el-input v-model="form.class8Analysis" disabled></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>
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

import infoForm from "@/views/components/infoForm.vue";

import {
  validateChangeQty,
  cxScheduleResultEdit,
  getInfoChangePlan,
} from "@/api/cx/cxScheduleResult";
import {
  changeQty,
  getScheduleDate,
} from "@/api/lh/scheduleResult";


export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      info: null,
      form: {},
      oForm: {},
      three1PlanTimeDisabled: false,
      three2PlanTimeDisabled: false,
      three3PlanTimeDisabled: false,
      three4PlanTimeDisabled: false,
      three5PlanTimeDisabled: false,
      two1PlanTimeDisabled: false,
      two2PlanTimeDisabled: false,
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      dateList:[
      {
            "shift": 1,
            "shiftDate": ""
        },
        {
            "shift": 2,
            "shiftDate": ""
        },
        {
            "shift": 3,
            "shiftDate": ""
        },
        {
            "shift": 4,
            "shiftDate": ""
        },
        {
            "shift": 5,
            "shiftDate": ""
        },
        {
            "shift": 6,
            "shiftDate": ""
        },
        {
            "shift": 7,
            "shiftDate": ""
        },
        {
            "shift": 8,
            "shiftDate": ""
        }
      ]
    };
  },
  computed: {
    title: function () {
      return this.$t("调量");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.taskType"),
          prop: "taskType",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineQty"),
          prop: "lhMachineQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.maximumClassQty"),
          prop: "maximumClassQty",
          span: 12,
          type: "number",
          min: 0,
          max: 9999,
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.minimumLhMachineReqQty"
          ),
          prop: "minimumLhMachineReqQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.workShifts"),
          prop: "workShifts",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.CLASS_SHIFT,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.availableMoldQty"),
          prop: "availableMoldQty",
          span: 12,
          disabled: true,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
          prop: "storageLocation",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.STORAGE_LOCATION,
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.embryoCode"),
          prop: "embryoCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specDesc"),
          prop: "specDesc",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.cxScheduleResult.sapCode"),
          prop: "sapCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.lhMiddleNightFinishQty"
          ),
          prop: "lhMiddleNightFinishQty",
          span: 12,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.class3PlannedQty"),
          prop: "class3PlannedQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.singleShiftLhQty"),
          prop: "singleShiftLhQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMonthFinishQty"),
          prop: "cxMonthFinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.monthPlan"),
          prop: "monthPlan",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.planModifyQty"),
          prop: "planModifyQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.totalStock"),
          prop: "totalStock",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.monthStock"),
          prop: "monthStock",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.rejectQty"),
          prop: "rejectQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.newestPlanQty"),
          prop: "newestPlanQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.actualOverProduction"
          ),
          prop: "actualOverProduction",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.expectedOverProduction"
          ),
          prop: "expectedOverProduction",
          span: 12,
          type: "number",
          min: -9999999,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.differenceOverProduction"
          ),
          prop: "differenceOverProduction",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.monthPlanOs"),
          prop: "monthPlanOs",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "monthPlanOs",
          span: 12,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.IS_RELEASE,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.specDimension"),
          prop: "specDimension",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },

        // ui.data.column.scheduleResult.class1
        {
          label: this.$t("ui.data.column.scheduleResult.class1"),
          type: "title",
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.class1AvailableLhShift"
          ),
          prop: "class1AvailableLhShift",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class1PlanQty",
          span: 12,
          disabled: true,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class1FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class1AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class1Analysis",
          span: 12,
          disabled: true,
        },

        //  ui.data.column.scheduleResult.class2
        {
          label: this.$t("ui.data.column.scheduleResult.class2"),
          type: "title",
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.class2AvailableLhShift"
          ),
          prop: "class2AvailableLhShift",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class2PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class2FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class2AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class2Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class3
        {
          label: this.$t("ui.data.column.scheduleResult.class3"),
          type: "title",
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.class3AvailableLhShift"
          ),
          prop: "class3AvailableLhShift",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class3PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class3FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class3AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class3Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class4
        {
          label: this.$t("ui.data.column.scheduleResult.class4"),
          type: "title",
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.class4AvailableLhShift"
          ),
          prop: "class4AvailableLhShift",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class4PlanQty",
          span: 12,
          disabled: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class4FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class4AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class4Analysis",
          span: 12,
          disabled: true,
        },

        // ui.data.column.scheduleResult.class5
        {
          label: this.$t("ui.data.column.scheduleResult.class5"),
          type: "title",
        },
        {
          label: this.$t(
            "ui.data.column.cxScheduleResult.class5AvailableLhShift"
          ),
          prop: "class5AvailableLhShift",
          span: 12,
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.scheduleResult.plan"),
          prop: "class5PlanQty",
          span: 12,
          disabled: this.three5PlanTimeDisabled,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.finish"),
          prop: "class5FinishQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analysis"),
          prop: "class5AnalysisInput",
          span: 12,
          maxlength: "66",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.analySystem"),
          prop: "class5Analysis",
          span: 12,
          disabled: true,
        },
      ];
    },
  },
  methods: {
    async getDate(date) {
      try {
        let res=await getScheduleDate({
          scheduleDate: date,
        });
        console.log(res);
        this.dateList=res
      } catch (error) {}
    },
    // api
    async getInfo(id) {
      try {
        this.loading = true;
        const res = await getInfoChangePlan({ id });
        this.form = res.cxScheduleResult;

        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    validateChangeQty(params) {
      return new Promise((resolve, reject) => {
        validateChangeQty(params)
          .then((res) => {
            if (res.msg) {
              this.$confirm(res.msg)
                .then(() => {
                  resolve();
                })
                .catch((e) => {
                  reject(e);
                });
            } else {
              resolve();
            }
          })
          .catch((e) => {
            reject(e);
          });
      });
    },

    async save(params) {
      try {
        this.loading = true;
        const res = await changeQty(params);
        this.loading = false;
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
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
        this.getDate(data.scheduleDate)
        // this.getInfo(data.id);

        if (data.scheduleDate) {
          if (moment().isAfter(data.scheduleDate)) {
            this.three1PlanTimeDisabled = true;
            this.two1PlanTimeDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 08:00:00")) {
            this.three2PlanTimeDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 12:00:00")) {
            this.two2PlanTimeDisabled = true;
          }
          if (moment().isAfter(data.scheduleDate + " 16:00:00")) {
            this.three3PlanTimeDisabled = true;
          }
          if (moment().isAfter(moment(data.scheduleDate).add(1, "days"))) {
            this.three4PlanTimeDisabled = true;
          }
          if (
            moment().isAfter(
              moment(data.scheduleDate + " 08:00:00").add(1, "days")
            )
          ) {
            this.three5PlanTimeDisabled = true;
          }
        } else {
          this.three1PlanTimeDisabled = true;
          this.three2PlanTimeDisabled = true;
          this.three3PlanTimeDisabled = true;
          this.three4PlanTimeDisabled = true;
          this.three5PlanTimeDisabled = true;
          this.two1PlanTimeDisabled = true;
          this.two2PlanTimeDisabled = true;
        }
      }
    },
    hide() {
      this.form = {};
      // this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
      this.three1PlanTimeDisabled = false;
      this.three2PlanTimeDisabled = false;
      this.three3PlanTimeDisabled = false;
      this.three4PlanTimeDisabled = false;
      this.three5PlanTimeDisabled = false;
      this.two1PlanTimeDisabled = false;
      this.two2PlanTimeDisabled = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save(this.form);
        }
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.content {
  width: 100%;
  height: 100%;
  overflow: auto;
}
</style>
