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
              :label="$t('ui.data.column.scheduleResult.scheduleDate')"
              prop="scheduleDate"
            >
              <el-date-picker
                class="w100"
                v-model="form.scheduleDate"
                type="date"
                value-format="yyyy-MM-dd"
                disabled
              ></el-date-picker>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.taskType')"
                  prop="taskType"
                >
                  <dict-select
                    v-model="form.taskType"
                    :options="parentDict.type.TASK_TYPE"
                    disabled
                  />
                </el-form-item>
              </el-col> -->
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.cxMachineCode')"
              prop="cxMachineCode"
            >
              <el-input v-model="form.cxMachineCode" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.lhMachineCode')"
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
              <el-input :value="displayMouldSurplusQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('胎胚库存')"
              prop="totalStock"
            >
              <el-input v-model="form.totalStock" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('硫化班产')"
              prop="lhClassQty"
            >
              <el-input v-model="form.lhClassQty" disabled></el-input>
            </el-form-item>
          </el-col>

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
              {{ getShiftTitle($t("早班"), 0) }}
            </h4>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class1PlanQty"
            >
              <el-input
                v-model="form.class1PlanQty"
                :disabled="three1PlanTimeDisabled || isShiftLocked(1)"
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
                :disabled="isShiftLocked(1)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class1RecipeNo">
              <embryoNoSelect
                :key="`class1RecipeNo-${form.class1RecipeNo || ''}`"
                v-model="form.class1RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(1, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class2RecipeNo">
              <embryoNoSelect
                :key="`class2RecipeNo-${form.class2RecipeNo || ''}`"
                v-model="form.class2RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(2, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class1RecipeType">
              <dict-select
                v-model="form.class1RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("中班"), 1) }}
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
                :disabled="three2PlanTimeDisabled || isShiftLocked(2)"
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
                :disabled="isShiftLocked(2)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class2RecipeNo">
              <embryoNoSelect
                :key="`class2RecipeNo-${form.class2RecipeNo || ''}`"
                v-model="form.class2RecipeNo"
                :materialCode="form.materialCode"
                :disabled="isShiftLocked(2)"
                @change="(val, row) => handleRecipeNoChange(2, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class2RecipeType">
              <dict-select
                v-model="form.class2RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("夜班"), 2) }}
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
                :disabled="three3PlanTimeDisabled || isShiftLocked(3)"
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
                :disabled="isShiftLocked(3)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class3RecipeNo">
              <embryoNoSelect
                :key="`class3RecipeNo-${form.class3RecipeNo || ''}`"
                v-model="form.class3RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(3, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class3RecipeType">
              <dict-select
                v-model="form.class3RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("早班"), 3) }}
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
                :disabled="three4PlanTimeDisabled || isShiftLocked(4)"
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
                :disabled="isShiftLocked(4)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class4RecipeNo">
              <embryoNoSelect
                :key="`class4RecipeNo-${form.class4RecipeNo || ''}`"
                v-model="form.class4RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(4, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class4RecipeType">
              <dict-select
                v-model="form.class4RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("中班"), 4) }}
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
                :disabled="three5PlanTimeDisabled || isShiftLocked(5)"
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
                :disabled="isShiftLocked(5)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class5RecipeNo">
              <embryoNoSelect
                :key="`class5RecipeNo-${form.class5RecipeNo || ''}`"
                v-model="form.class5RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(5, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class5RecipeType">
              <dict-select
                v-model="form.class5RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("夜班"), 5) }}
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
                :disabled="three6PlanTimeDisabled || isShiftLocked(6)"
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
                :disabled="isShiftLocked(6)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class6RecipeNo">
              <embryoNoSelect
                :key="`class6RecipeNo-${form.class6RecipeNo || ''}`"
                v-model="form.class6RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(6, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class6RecipeType">
              <dict-select
                v-model="form.class6RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>


          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("早班"), 6) }}
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
                :disabled="three7PlanTimeDisabled || isShiftLocked(7)"
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
                :disabled="isShiftLocked(7)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class7RecipeNo">
              <embryoNoSelect
                :key="`class7RecipeNo-${form.class7RecipeNo || ''}`"
                v-model="form.class7RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(7, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class7RecipeType">
              <dict-select
                v-model="form.class7RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <h4 class="form-header h4">
              {{ getShiftTitle($t("中班"), 7) }}
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
                :disabled="three8PlanTimeDisabled || isShiftLocked(8)"
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
                :disabled="isShiftLocked(8)"
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
          <el-col :span="12">
            <el-form-item :label="$t('示方编号')" prop="class8RecipeNo">
              <embryoNoSelect
                :key="`class8RecipeNo-${form.class8RecipeNo || ''}`"
                v-model="form.class8RecipeNo"
                :materialCode="form.materialCode"
                disabled
                @change="(val, row) => handleRecipeNoChange(8, val, row)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('示方类型')" prop="class8RecipeType">
              <dict-select
                v-model="form.class8RecipeType"
                :options="parentDict.type.trial_status"
                disabled
              />
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
import embryoNoSelect from "@/views/components/embryoNoSelect.vue";

import {
  validateChangeQty,
  adjustQty,
  getInfoChangePlan,
} from "@/api/cx/cxScheduleResult";
import { getScheduleDate } from "@/api/lh/scheduleResult";

export default {
  components: { infoForm, embryoNoSelect },
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
      three6PlanTimeDisabled: false,
      three7PlanTimeDisabled: false,
      three8PlanTimeDisabled: false,
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
    displayMouldSurplusQty() {
      if (this.form.mouldSurplusQty !== undefined && this.form.mouldSurplusQty !== null) {
        return this.form.mouldSurplusQty;
      }
      if (this.form.cxRemainQty !== undefined && this.form.cxRemainQty !== null) {
        return this.form.cxRemainQty;
      }
      if (this.form.lhRemainQty !== undefined && this.form.lhRemainQty !== null) {
        return this.form.lhRemainQty;
      }
      return "";
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
    buildAdjustQtyParams(form = {}) {
      const params = {
        id: Number(form.id) || 0,
      };
      for (let index = 1; index <= 8; index += 1) {
        params[`class${index}AnalysisInput`] = form[`class${index}AnalysisInput`] || "";
        params[`class${index}FinishQty`] = Number(form[`class${index}FinishQty`]) || 0;
        params[`class${index}PlanQty`] = Number(form[`class${index}PlanQty`]) || 0;
        params[`class${index}RecipeNo`] = form[`class${index}RecipeNo`] || "";
        params[`class${index}RecipeType`] = form[`class${index}RecipeType`] || "";
      }
      return params;
    },
    async getShiftDates(scheduleDate) {
      if (!scheduleDate) {
        return;
      }
      try {
        const res = await getScheduleDate({ scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },
    getShiftTitle(label, shiftIndex) {
      const shiftDate = this.getShiftDateByIndex(shiftIndex);
      return shiftDate ? `${label} ${shiftDate}` : label;
    },
    getShiftDateByIndex(shiftIndex) {
      return this.formatShiftDate(this.dateList?.[shiftIndex]?.shiftDate);
    },
    formatShiftDate(date) {
      if (!date) {
        return "";
      }
      if (/^\d{2}\/\d{2}$/.test(date)) {
        return date;
      }
      return moment(date).format("MM/DD");
    },
    isShiftLocked(shift) {
      const endTime = this.form[`class${shift}EndTime`];
      return endTime ? moment().isAfter(moment(endTime)) : false;
    },
    handleRecipeNoChange(shift, val, row) {
      const recipeTypeField = `class${shift}RecipeType`;
      if (val && row) {
        this.$set(this.form, recipeTypeField, row.trialStatus);
      } else {
        this.$set(this.form, recipeTypeField, "");
      }
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
    validatePlanQtyByFinishQty() {
      for (let i = 1; i <= 8; i++) {
        const planQty = Number(this.form[`class${i}PlanQty`] || 0);
        const finishQty = Number(this.form[`class${i}FinishQty`] || 0);
        if (finishQty > 0 && planQty < finishQty) {
          this.$modal.msgError(`第${i}班次计划量（${planQty}）不能低于已完成量（${finishQty}）`);
          return false;
        }
      }
      return true;
    },

    async save(form) {
      try {
        this.loading = true;
        const params = this.buildAdjustQtyParams(form);
        await adjustQty(params);
        this.loading = false;
        this.$modal.msgSuccess(this.$t("common.msg.ajax.operation.success"));
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
        // this.getInfo(data.id);
        this.getShiftDates(data.scheduleDate);

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
      this.dateList = Array.from({ length: 8 }, (_, index) => ({
        shift: index + 1,
        shiftDate: "",
      }));
      // this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
      this.three1PlanTimeDisabled = false;
      this.three2PlanTimeDisabled = false;
      this.three3PlanTimeDisabled = false;
      this.three4PlanTimeDisabled = false;
      this.three5PlanTimeDisabled = false;
      this.three6PlanTimeDisabled = false;
      this.three7PlanTimeDisabled = false;
      this.three8PlanTimeDisabled = false;
      this.two1PlanTimeDisabled = false;
      this.two2PlanTimeDisabled = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          if (!this.validatePlanQtyByFinishQty()) {
            return;
          }
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
::v-deep .el-input__inner {
  text-align: left;
}
</style>
