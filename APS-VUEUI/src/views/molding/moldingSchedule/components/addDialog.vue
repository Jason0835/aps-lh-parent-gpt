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

          <el-col :span="12">
            <el-form-item
              :label="$t('common.factory')"
              prop="factoryCode"
            >
              <dict-select
                style="width: 100%"
                v-model="form.factoryCode"
                :options="parentDict.type.biz_factory_name"
                filterable
                clearable
              />
            </el-form-item>
          </el-col>



          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.cxMachineCode')"
              prop="cxMachineCode"
            >
              <el-select
                style="width: 100%"
                v-model="form.cxMachineCode"
                filterable
                clearable
                placeholder=""
                @change="handleCxMachineChange"
              >
                <el-option
                  v-for="m in moldingMachines"
                  :key="m.cxMachineCode"
                  :label="m.cxMachineCode"
                  :value="m.cxMachineCode"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.lhMachineCode')"
              prop="lhMachineCode"
            >
              <el-select
                style="width: 100%"
                v-model="form.lhMachineCode"
                multiple
                filterable
                clearable
                placeholder=""
              >
                <el-option
                  v-for="m in curingMachines"
                  :key="m.machineCode"
                  :label="m.machineCode"
                  :value="m.machineCode"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('物料编码')" prop="materialCode">
              <materialCodeSelect
                :key="form.materialCode"
                v-model="form.materialCode"
                :disabled="false"
                @change="handleMaterialCodeChange"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('物料描述')" prop="materialDesc">
              <el-input v-model="form.materialDesc" disabled></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.embryoCode')"
              prop="embryoCode"
            >
              <el-input
                v-model="form.embryoCode"
                placeholder=""
              ></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('胎胚描述')" prop="mainMaterialDesc">
              <el-input v-model="form.mainMaterialDesc"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('合计余量')" prop="mouldSurplusQty">
              <el-input v-model="form.mouldSurplusQty"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('胎胚库存')" prop="totalStock">
              <el-input v-model="form.totalStock"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="$t('硫化班产')" prop="lhClassQty">
              <el-input v-model="form.lhClassQty"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("早班"), 0) }}
          </h4>
        </el-col>
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class1PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class1PlanQty"
                :min="0"
                :disabled="isClassPast(1)"
                controls-position="right"
              ></el-input-number>
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
                :disabled="isClassPast(1)"
                @input="handleAnalysisInputChange(1)"
              ></el-input>
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
                :disabled="isClassPast(2)"
                @input="handleAnalysisInputChange(2)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("夜班"), 2) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class3PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class3PlanQty"
                :min="0"
                :disabled="isClassPast(3)"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.finish')"
              prop="class3FinishQty"
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
                :disabled="isClassPast(3)"
                @input="handleAnalysisInputChange(3)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("早班"), 3) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class4PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class4PlanQty"
                :min="0"
                :disabled="isClassPast(4)"
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
                :disabled="isClassPast(4)"
                @input="handleAnalysisInputChange(4)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("中班"), 4) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class5PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class5PlanQty"
                :min="0"
                :disabled="isClassPast(5)"
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
                :disabled="isClassPast(5)"
                @input="handleAnalysisInputChange(5)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("夜班"), 5) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class6PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class6PlanQty"
                :min="0"
                :disabled="isClassPast(6)"
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
                :disabled="isClassPast(6)"
                @input="handleAnalysisInputChange(6)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("早班"), 6) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class7PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class7PlanQty"
                :min="0"
                :disabled="isClassPast(7)"
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
                :disabled="isClassPast(7)"
                @input="handleAnalysisInputChange(7)"
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
                :disabled="!form.materialCode"
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
        </el-row>

        <el-col :span="24">
          <h4 class="form-header h4">
            {{ getShiftTitle($t("中班"), 7) }}
          </h4>
        </el-col>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.plan')"
              prop="class8PlanQty"
            >
              <el-input-number
                class="w100"
                v-model="form.class8PlanQty"
                :min="0"
                :disabled="isClassPast(8)"
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
                :disabled="isClassPast(8)"
                @input="handleAnalysisInputChange(8)"
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
                :disabled="!form.materialCode"
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
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import { mapState } from "vuex";

import { insertOrder } from "@/api/cx/cxScheduleResult";
import { getScheduleDate } from "@/api/lh/scheduleResult";

import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import embryoNoSelect from "@/views/components/embryoNoSelect.vue";

export default {
  components: { materialCodeSelect, embryoNoSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      dateList: Array.from({ length: 8 }, (_, i) => ({
        shift: i + 1,
        shiftDate: "",
      })),
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
      curingMachines: (state) => state.curing.machines,
    }),
    title() {
      return this.$t("ui.data.column.scheduleResult.insertOrder");
    },
  },
  methods: {
    getShiftTitle(label, shiftIndex) {
      const shiftDate = this.getShiftDateByIndex(shiftIndex);
      return shiftDate ? `${label} ${shiftDate}` : label;
    },
    getShiftDateByIndex(shiftIndex) {
      // 跟 moldingSchedule/index.vue 一致：直接使用接口返回的 shiftDate 展示
      return this.dateList?.[shiftIndex]?.shiftDate || "";
    },
    /**
     * 判断班次是否已过（插单时置灰不可输入）
     * scheduleDate 为 T+2 日，CLASS1~8 分别对应 D1早~D3中
     */
    isClassPast(classIndex) {
      if (!this.form.scheduleDate) return false;
      const scheduleDate = moment(this.form.scheduleDate, "YYYY-MM-DD");
      if (!scheduleDate.isValid()) return false;
      const now = moment();
      // 各班的结束时间（时:分）
      const shifts = {
        1: { dayOffset: -2, hour: 14, min: 0 },   // D1早班
        2: { dayOffset: -2, hour: 22, min: 0 },   // D1中班
        3: { dayOffset: -1, hour: 6, min: 0 },    // D2夜班
        4: { dayOffset: -1, hour: 14, min: 0 },   // D2早班
        5: { dayOffset: -1, hour: 22, min: 0 },   // D2中班
        6: { dayOffset: 0, hour: 6, min: 0 },     // D3夜班
        7: { dayOffset: 0, hour: 14, min: 0 },    // D3早班
        8: { dayOffset: 0, hour: 22, min: 0 },    // D3中班
      };
      const s = shifts[classIndex];
      if (!s) return false;
      const end = scheduleDate.clone().add(s.dayOffset, "days").hour(s.hour).minute(s.min).second(0);
      return now.isSameOrAfter(end);
    },
    formatShiftDate(date) {
      if (!date) return "";
      if (/^\d{2}\/\d{2}$/.test(date)) return date;
      return moment(date).format("MM/DD");
    },

    handleCxMachineChange(val) {
      const machine = (this.moldingMachines || []).find(
        (m) => m.cxMachineCode === val
      );
      this.$set(
        this.form,
        "cxMachineName",
        machine?.cxMachineName || machine?.machineName || val || ""
      );
    },

    handleMaterialCodeChange(val, row) {
      if (val && row) {
        this.$set(this.form, "materialDesc", row.materialDesc || "");

        // 尽量从 productinfo 行字段映射到插单需要的字段
        const embryoCode = row.mesMaterialCode || row.embryoCode || val;
        // this.$set(this.form, "embryoCode", embryoCode ? String(embryoCode) : "");
        // this.$set(this.form, "mainMaterialDesc", row.mainMaterialDesc || row.mainMaterialDesc || "");
        this.$set(this.form, "specDesc", row.specifications || row.specDesc || "");

        const dim = row.proSize ?? row.specDimension;
        this.$set(this.form, "specDimension", dim === undefined || dim === null || dim === "" ? 0 : Number(dim));

        this.$set(this.form, "structureName", row.hierarchy || row.structureName || "");
      } else {
        this.$set(this.form, "materialDesc", "");
        this.$set(this.form, "embryoCode", "");
        this.$set(this.form, "specDesc", "");
        this.$set(this.form, "specDimension", 0);
        this.$set(this.form, "structureName", "");
      }

      // 示方编号依赖 materialCode，清空已选示方
      for (let i = 1; i <= 8; i += 1) {
        this.$set(this.form, `class${i}RecipeNo`, "");
        this.$set(this.form, `class${i}RecipeType`, "");
      }
    },

    handleRecipeNoChange(shift, val, row) {
      const recipeTypeField = `class${shift}RecipeType`;
      if (val && row) {
        this.$set(this.form, recipeTypeField, row.trialStatus || "");
      } else {
        this.$set(this.form, recipeTypeField, "");
      }
    },

    handleAnalysisInputChange(shift) {
      const inputField = `class${shift}AnalysisInput`;
      const sysField = `class${shift}Analysis`;
      this.$set(this.form, sysField, this.form[inputField] || "");
    },

    async getShiftDates(scheduleDate) {
      if (!scheduleDate) return;
      try {
        const res = await getScheduleDate({ scheduleDate });
        if (Array.isArray(res) && res.length) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },

    show(data = {}) {
      this.visible = true;

      const scheduleDate = data?.scheduleDate
        ? String(data.scheduleDate)
        : moment().add(1, "days").format("YYYY-MM-DD");

      const empty = {};
      for (let i = 1; i <= 8; i += 1) {
        empty[`class${i}PlanQty`] = 0;
        empty[`class${i}FinishQty`] = 0;
        empty[`class${i}AnalysisInput`] = "";
        empty[`class${i}Analysis`] = "";
        empty[`class${i}RecipeNo`] = "";
        empty[`class${i}RecipeType`] = "";
      }

      this.form = {
        scheduleDate,

        factoryCode: "116",
        cxMachineCode: "",
        cxMachineName: "",
        cxBatchNo: "",
        cxRemainQty: 0,

        lhMachineCode: [],
        lhMachineQty: 0,
        lhClassQty: 0,
        lhRemainQty: 0,

        materialCode: "",
        materialDesc: "",
        mainMaterialDesc: "",
        embryoCode: "",
        specDesc: "",
        specDimension: 0,
        structureName: "",
        exampleNo: "",
        mouldSurplusQty: 0,
        totalStock: 0,
        remark: "",

        ...empty,
      };

      this.getShiftDates(this.form.scheduleDate);
    },

    hide() {
      this.visible = false;
      this.loading = false;
      this.form = {};
      this.dateList = Array.from({ length: 8 }, (_, i) => ({
        shift: i + 1,
        shiftDate: "",
      }));
    },

    handleConfirm() {
      this.save();
    },

    toScheduleDateIso(scheduleDate) {
      try {
        // scheduleDate 通常是 yyyy-MM-dd
        return new Date(scheduleDate).toISOString();
      } catch (e) {
        return new Date().toISOString();
      }
    },

    async save() {
      try {
        this.loading = true;

        const lhMachineCode =
          Array.isArray(this.form.lhMachineCode) && this.form.lhMachineCode.length
            ? this.form.lhMachineCode.join(",")
            : "";

        const payload = {
          ...this.form,
          scheduleDate: this.toScheduleDateIso(this.form.scheduleDate),
          lhMachineCode,
        };

        // payload 按接口字段命名整理（class1..class8）
        for (let i = 1; i <= 8; i += 1) {
          const analysisInput = payload[`class${i}AnalysisInput`] || "";
          payload[`class${i}Analysis`] =
            payload[`class${i}Analysis`] || analysisInput || "";
          payload[`class${i}PlanQty`] = Number(payload[`class${i}PlanQty`]) || 0;
          payload[`class${i}FinishQty`] =
            Number(payload[`class${i}FinishQty`]) || 0;
          payload[`class${i}RecipeNo`] = String(payload[`class${i}RecipeNo`] || "");
          payload[`class${i}RecipeType`] = String(
            payload[`class${i}RecipeType`] || ""
          );
        }

        // 兜底数值字段
        payload.cxRemainQty = Number(payload.cxRemainQty) || 0;
        payload.lhMachineQty = Number(payload.lhMachineQty) || 0;
        payload.lhClassQty = Number(payload.lhClassQty) || 0;
        payload.lhRemainQty = Number(payload.lhRemainQty) || 0;
        payload.specDimension = Number(payload.specDimension) || 0;
        payload.totalStock = Number(payload.totalStock) || 0;

        // 发送插单接口
        await insertOrder(payload);
        this.$modal.msgSuccess(this.$t("common.msg.ajax.operation.success"));
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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

