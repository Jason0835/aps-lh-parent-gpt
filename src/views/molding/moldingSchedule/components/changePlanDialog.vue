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
        <!-- <el-tabs>
          <el-tab-pane :label="$t('ui.data.column.scheduleResult.cxChangeQty')">
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
                  :label="$t('ui.data.column.cxScheduleResult.taskType')"
                  prop="taskType"
                >
                  <dict-select
                    v-model="form.taskType"
                    :options="parentDict.type.TASK_TYPE"
                    disabled
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.cxMachineCode')"
                  prop="cxMachineCode"
                >
                  <el-input v-model="form.cxMachineName" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.lhMachineCode')"
                  prop="lhMachineCode"
                >
                  <el-input v-model="form.lhMachineName" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.lhMachineQty')"
                  prop="lhMachineQty"
                >
                  <el-input v-model="form.lhMachineQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.maximumClassQty')"
                  prop="maximumClassQty"
                >
                  <el-input v-model="form.maximumClassQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
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
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.workShifts')"
                  prop="workShifts"
                >
                  <dict-select
                    v-model="form.workShifts"
                    :options="parentDict.type.CLASS_SHIFT"
                    disabled
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.availableMoldQty')
                  "
                  prop="availableMoldQty"
                >
                  <el-input v-model="form.availableMoldQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.storageLocation')"
                  prop="storageLocation"
                >
                  <dict-select
                    v-model="form.storageLocation"
                    :options="parentDict.type.STORAGE_LOCATION"
                    disabled
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.productConstruction.embryoVersion')
                  "
                  prop="embryoVersion"
                >
                  <el-input v-model="form.embryoVersion" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.embryoCode')"
                  prop="embryoCode"
                >
                  <el-input v-model="form.embryoCode" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.specDesc')"
                  prop="specDesc"
                >
                  <el-input v-model="form.specDesc" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.sapCode')"
                  prop="sapCode"
                >
                  <el-input v-model="form.sapCode" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.lhMiddleNightFinishQty')
                  "
                  prop="lhMiddleNightFinishQty"
                >
                  <el-input
                    v-model="form.lhMiddleNightFinishQty"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class3PlannedQty')
                  "
                  prop="class3PlannedQty"
                >
                  <el-input v-model="form.class3PlannedQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.singleShiftLhQty')
                  "
                  prop="singleShiftLhQty"
                >
                  <el-input v-model="form.singleShiftLhQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.cxMonthFinishQty')
                  "
                  prop="cxMonthFinishQty"
                >
                  <el-input v-model="form.cxMonthFinishQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.scheduleResult.monthPlan')"
                  prop="monthPlan"
                >
                  <el-input v-model="form.monthPlan" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.planModifyQty')"
                  prop="planModifyQty"
                >
                  <el-input v-model="form.planModifyQty" disabled></el-input>
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
                  :label="$t('ui.data.column.cxScheduleResult.monthStock')"
                  prop="monthStock"
                >
                  <el-input v-model="form.monthStock" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.rejectQty')"
                  prop="rejectQty"
                >
                  <el-input v-model="form.rejectQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.newestPlanQty')"
                  prop="newestPlanQty"
                >
                  <el-input v-model="form.newestPlanQty" disabled></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.actualOverProduction')
                  "
                  prop="actualOverProduction"
                >
                  <el-input
                    v-model="form.actualOverProduction"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.expectedOverProduction')
                  "
                  prop="expectedOverProduction"
                >
                  <el-input-number
                    class="w100"
                    v-model="form.expectedOverProduction"
                    :min="-9999999"
                    :max="9999999"
                    :precision="0"
                    controls-position="right"
                  ></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t(
                      'ui.data.column.cxScheduleResult.differenceOverProduction'
                    )
                  "
                  prop="differenceOverProduction"
                >
                  <el-input
                    v-model="form.differenceOverProduction"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.scheduleResult.monthPlanOs')"
                  prop="monthPlanOs"
                >
                  <el-input v-model="form.monthPlanOs" disabled></el-input>
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
              </el-col>
              <el-col :span="24">
                <el-form-item
                  :label="$t('ui.common.column.remark')"
                  prop="remark"
                >
                  <el-input
                    type="textarea"
                    v-model="form.remark"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <h4 class="form-header h4">
                  {{ $t("ui.data.column.scheduleResult.class1") }}
                </h4>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class1AvailableLhShift')
                  "
                  prop="class1AvailableLhShift"
                >
                  <el-input
                    v-model="form.class1AvailableLhShift"
                    disabled
                  ></el-input>
                </el-form-item>
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
                  {{ $t("ui.data.column.scheduleResult.class2") }}
                </h4>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class2AvailableLhShift')
                  "
                  prop="class2AvailableLhShift"
                >
                  <el-input
                    v-model="form.class2AvailableLhShift"
                    disabled
                  ></el-input>
                </el-form-item>
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
                  {{ $t("ui.data.column.scheduleResult.class3") }}
                </h4>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class3AvailableLhShift')
                  "
                  prop="class3AvailableLhShift"
                >
                  <el-input
                    v-model="form.class3AvailableLhShift"
                    disabled
                  ></el-input>
                </el-form-item>
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
                  {{ $t("ui.data.column.scheduleResult.class4") }}
                </h4>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class4AvailableLhShift')
                  "
                  prop="class4AvailableLhShift"
                >
                  <el-input
                    v-model="form.class4AvailableLhShift"
                    disabled
                  ></el-input>
                </el-form-item>
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
                  {{ $t("ui.data.column.scheduleResult.class5") }}
                </h4>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.class5AvailableLhShift')
                  "
                  prop="class5AvailableLhShift"
                >
                  <el-input
                    v-model="form.class5AvailableLhShift"
                    disabled
                  ></el-input>
                </el-form-item>
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
            </el-row>
          </el-tab-pane>
          <el-tab-pane
            :label="$t('ui.data.column.scheduleResult.otherChangeQty')"
          >
            <template
              v-if="form.tmScheduleList && form.tmScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.tmPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.tmScheduleList"
                :key="`tm-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.tmScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.tmScheduleList[index].tmDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.tmScheduleList[index].tmNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.tcScheduleLis && form.tcScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.tcPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.tcScheduleList"
                :key="`tm-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.tcScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.tcScheduleList[index].tcDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.tcScheduleList[index].tcNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.ncScheduleList && form.ncScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.ncPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.ncScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.ncScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.ncScheduleList[index].ncDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.ncScheduleList[index].ncNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.tqScheduleList && form.tqScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.tqPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.tqScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.tqScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="three1PlanTimeDisabled"
                      v-model="form.tqScheduleList[index].tqMidPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="three2PlanTimeDisabled"
                      v-model="form.tqScheduleList[index].tqNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.midPlanQty')"
                  >
                    <el-input
                      :disabled="three3PlanTimeDisabled"
                      v-model="form.tqScheduleList[index].tqDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nextMidPlanQty')"
                  >
                    <el-input
                      :disabled="three4PlanTimeDisabled"
                      v-model="form.tqScheduleList[index].tqNextMidPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.gsqScheduleList && form.gsqScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.gsqPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.gsqScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.gsqScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="three1PlanTimeDisabled"
                      v-model="form.gsqScheduleList[index].gsqMidPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="three2PlanTimeDisabled"
                      v-model="form.gsqScheduleList[index].gsqNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.midPlanQty')"
                  >
                    <el-input
                      :disabled="three3PlanTimeDisabled"
                      v-model="form.gsqScheduleList[index].gsqDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.cd15ScheduleList && form.cd15ScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.cd15Plan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.cd15ScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.cd15ScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.cd15ScheduleList[index].cd15DayPlanQty1"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.cd15ScheduleList[index].cd15NightPlanQty1"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.cd90ScheduleList && form.cd90ScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.cd90Plan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.cd90ScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.cd90ScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.cd90ScheduleList[index].cd90DayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.cd90ScheduleList[index].cd90NightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.gdyyScheduleList && form.gdyyScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.gdyyPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.gdyyScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.gdyy.scheduleResult.class1Plan')"
                  >
                    <el-input
                      :disabled="three1PlanTimeDisabled"
                      v-model="form.gdyyScheduleList[index].gdyyClass1Plan"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.gdyy.scheduleResult.class2Plan')"
                  >
                    <el-input
                      :disabled="three2PlanTimeDisabled"
                      v-model="form.gdyyScheduleList[index].gdyyClass2Plan"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.gdyy.scheduleResult.class3Plan')"
                  >
                    <el-input
                      :disabled="three3PlanTimeDisabled"
                      v-model="form.gdyyScheduleList[index].gdyyClass3Plan"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <template
              v-if="form.xwyyScheduleList && form.xwyyScheduleList.length !== 0"
            >
              <h4 class="form-header h4">
                {{ $t("ui.data.column.scheduleResult.xwyyPlan") }}
              </h4>
              <el-row
                v-for="(item, index) in form.xwyyScheduleList"
                :key="`nc-${index}`"
              >
                <div>{{ item.osLable }}</div>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.cx.machine.machineName')"
                  >
                    <el-input
                      v-model="form.xwyyScheduleList[index].machineName"
                      disabled
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.dayPlanQty')"
                  >
                    <el-input
                      :disabled="two1PlanTimeDisabled"
                      v-model="form.xwyyScheduleList[index].xwyyDayPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    :label="$t('ui.data.column.scheduleResult.nightPlanQty')"
                  >
                    <el-input
                      :disabled="two2PlanTimeDisabled"
                      v-model="form.xwyyScheduleList[index].xwyyNightPlanQty"
                    ></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
          </el-tab-pane>
        </el-tabs> -->
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
              <el-input v-model="form.cxMachineName" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.lhMachineCode')"
              prop="lhMachineCode"
            >
              <el-input v-model="form.lhMachineName" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.lhMachineQty')"
              prop="lhMachineQty"
            >
              <el-input v-model="form.lhMachineQty" disabled></el-input>
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
              :label="$t('ui.data.column.cxScheduleResult.workShifts')"
              prop="workShifts"
            >
              <dict-select
                v-model="form.workShifts"
                :options="parentDict.type.CLASS_SHIFT"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.availableMoldQty')"
              prop="availableMoldQty"
            >
              <el-input v-model="form.availableMoldQty" disabled></el-input>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
                <el-form-item
                  :label="$t('ui.data.column.cxScheduleResult.storageLocation')"
                  prop="storageLocation"
                >
                  <dict-select
                    v-model="form.storageLocation"
                    :options="parentDict.type.STORAGE_LOCATION"
                    disabled
                  />
                </el-form-item>
              </el-col> -->
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.productConstruction.embryoVersion')"
              prop="embryoVersion"
            >
              <el-input v-model="form.embryoVersion" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.embryoCode')"
              prop="embryoCode"
            >
              <el-input v-model="form.embryoCode" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.specDesc')"
              prop="specDesc"
            >
              <el-input v-model="form.specDesc" disabled></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.sapCode')"
              prop="sapCode"
            >
              <el-input v-model="form.sapCode" disabled></el-input>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
                <el-form-item
                  :label="
                    $t('ui.data.column.cxScheduleResult.lhMiddleNightFinishQty')
                  "
                  prop="lhMiddleNightFinishQty"
                >
                  <el-input
                    v-model="form.lhMiddleNightFinishQty"
                    disabled
                  ></el-input>
                </el-form-item>
              </el-col> -->
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
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.monthPlan')"
              prop="monthPlan"
            >
              <el-input v-model="form.monthPlan" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.planModifyQty')"
              prop="planModifyQty"
            >
              <el-input v-model="form.planModifyQty" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.totalStock')"
              prop="totalStock"
            >
              <el-input v-model="form.totalStock" disabled></el-input>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.monthStock')"
              prop="monthStock"
            >
              <el-input v-model="form.monthStock" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.rejectQty')"
              prop="rejectQty"
            >
              <el-input v-model="form.rejectQty" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.cxScheduleResult.newestPlanQty')"
              prop="newestPlanQty"
            >
              <el-input v-model="form.newestPlanQty" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.actualOverProduction')
              "
              prop="actualOverProduction"
            >
              <el-input v-model="form.actualOverProduction" disabled></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.expectedOverProduction')
              "
              prop="expectedOverProduction"
            >
              <el-input-number
                class="w100"
                v-model="form.expectedOverProduction"
                :min="-9999999"
                :max="9999999"
                :precision="0"
                controls-position="right"
              ></el-input-number>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.differenceOverProduction')
              "
              prop="differenceOverProduction"
            >
              <el-input
                v-model="form.differenceOverProduction"
                disabled
              ></el-input>
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="12">
            <el-form-item
              :label="$t('ui.data.column.scheduleResult.monthPlanOs')"
              prop="monthPlanOs"
            >
              <el-input v-model="form.monthPlanOs" disabled></el-input>
            </el-form-item>
          </el-col> -->
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
              {{ $t("ui.data.column.scheduleResult.class2") }}
            </h4>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.class1AvailableLhShift')
              "
              prop="class1AvailableLhShift"
            >
              <el-input
                v-model="form.class1AvailableLhShift"
                disabled
              ></el-input>
            </el-form-item>
          </el-col> -->
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
              {{ $t("ui.data.column.scheduleResult.class3") }}
            </h4>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.class2AvailableLhShift')
              "
              prop="class2AvailableLhShift"
            >
              <el-input
                v-model="form.class2AvailableLhShift"
                disabled
              ></el-input>
            </el-form-item>
          </el-col> -->
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
              {{ $t("ui.data.column.scheduleResult.class4") }}
            </h4>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.class4AvailableLhShift')
              "
              prop="class4AvailableLhShift"
            >
              <el-input
                v-model="form.class4AvailableLhShift"
                disabled
              ></el-input>
            </el-form-item>
          </el-col> -->
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
              {{ $t("ui.data.column.scheduleResult.class5") }}
            </h4>
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item
              :label="
                $t('ui.data.column.cxScheduleResult.class5AvailableLhShift')
              "
              prop="class5AvailableLhShift"
            >
              <el-input
                v-model="form.class5AvailableLhShift"
                disabled
              ></el-input>
            </el-form-item>
          </el-col> -->
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
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.cxAutoPlan");
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
        await validateChangeQty(params);
        const res = await cxScheduleResultEdit({
          ...this.form,
          local: "tl",
        });
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
