<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
      key="cxFixedMachineMainTable"
      ref="tableRef"
      :calcHeight="showOutResult && displayOutResultDetailTable ? false : true"
      v-loading="loading"
      :element-loading-text="loadText"
      :row-class-name="tableRowClassName"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @reset="refreshSearch"
      :isReset="true"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      row-key="id"
      :expand-row-keys="expands"
      @expand-change="handleExpandChange"
      :max-height="
        showOutResult && displayOutResultDetailTable ? 450 : 'calc(100vh )'
      "
    >
      <template slot="header">
        <!-- 结构内调整页：仅结构内工具栏 -->
        <div
          v-if="isStructureInnerPage"
          class="mp-structure-inner-header"
        >
          <div class="mp-structure-inner-toolbar">
            <el-button
              @click="adjustOrder"
              :loading="getLoading"
              v-hasPermi="['monthplan:mpWeekRollAdjust:getAdjustDetailList']"
              >{{ $t("获取调整订单") }}</el-button
            >
            <el-button
              @click="handShowResult"
              :loading="autoLoading"
              :disabled="data.length == 0"
              v-hasPermi="['monthplan:mpWeekRollAdjust:autoAdjust']"
              >{{ $t("自动调整") }}</el-button
            >
          </div>
        </div>
        <!-- 结构调整页：列表工具栏或单结构流程表单 -->
        <div
          v-else-if="isStructureAdjustPage"
          class="mp-structure-inner-header"
        >
          <div v-if="!isShowResult" class="mp-structure-inner-toolbar">
            <el-button @click="handleAdd" :disabled="selection.length != 1">{{
              $t("单选结构调整")
            }}</el-button>
            <el-button
              @click="handleAddSpecial"
              v-hasPermi="['monthplan:mpStructureAllocation:save']"
              >{{ $t("新增结构") }}</el-button
            >
          </div>
          <el-form
            v-else
            :inline="true"
            :model="formInline"
            class="demo-form-inline"
          >
            <el-form-item
              :label="this.$t('ui.data.column.monthPlanStructureAdjust.cxMachineCode')"
            >
              <el-input
                v-model="formInline.cxMachineCode"
                disabled
                :placeholder="
                  this.$t('ui.data.column.cxScheduleResult.cxMachineCode')
                "
              ></el-input>
            </el-form-item>
            <el-form-item
              :label="this.$t('ui.data.column.finishStock.structureName')"
            >
              <el-input
                v-model="formInline.structureName"
                disabled
                :placeholder="
                  this.$t('ui.data.column.finishStock.structureName')
                "
              ></el-input>
            </el-form-item>
            <el-form-item
              :label="
                this.$t(
                  'ui.data.column.monthPlanFinalAdjustQuery.relatedMachine'
                )
              "
            >
              <el-input
                v-model="formInline.scheduledMachines"
                disabled
                :placeholder="
                  this.$t(
                    'ui.data.column.monthPlanFinalAdjustQuery.relatedMachine'
                  )
                "
              />
            </el-form-item>

            <el-form-item :label="this.$t('common.startDate')">
              <el-input
                disabled
                v-model="formInline.beginDay"
                style="width: 50px"
                :placeholder="this.$t('common.startDate')"
              ></el-input>
            </el-form-item>
            <el-form-item :label="this.$t('common.endDate')">
              <el-input
                disabled
                style="width: 50px"
                v-model="formInline.endDay"
                :placeholder="this.$t('common.endDate')"
              ></el-input>
            </el-form-item>
            <!-- 调整日期与操作按钮单独一行 -->
            <div class="mp-roll-adjust-toolbar-line">
              <el-form-item label="调整开始日期">
                <el-select
                  v-model="formInline.adjustStartDay"
                  style="width: 100px"
                  disabled
                  filterable
                >
                  <el-option
                    v-for="item in dayList"
                    :key="item"
                    :label="item"
                    :value="item"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="调整结束日期">
                <el-select
                  v-model="formInline.adjustEndDay"
                  style="width: 100px"
                  filterable
                >
                  <el-option
                    v-for="item in dayList"
                    :key="item"
                    :label="item"
                    :value="item"
                  >
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  @click="getOutList"
                  v-hasPermi="[
                    'monthplan:mpWeekRollAdjust:getAdjustDetailList',
                  ]"
                  >获取调整订单</el-button
                >
              </el-form-item>
              <!-- <el-button
                @click="handShowResult"
                :loading="autoLoading"
                :disabled="data.length == 0"
                v-hasPermi="['monthplan:mpWeekRollAdjust:autoAdjust']"
                >{{ $t("自动调整") }}</el-button
              > -->
              <!-- <el-form-item v-if="showOutResult">
                <el-button
                  type="primary"
                  @click="nextStructure"
                  :loading="nextLoading"
                  >下一个</el-button
                >
              </el-form-item> -->
            </div>
          </el-form>
        </div>
      </template>
      <template slot="footer" v-if="isShowFoot">
        <div
          style="
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
          "
        >
          <el-button @click="backPlan">
            {{ this.$t("common.button.cancel") }}</el-button
          >
          <el-button
            type="primary"
            @click="confirmResult"
            v-if="activeName != 'singleResult'"
            :loading="loading"
            :disabled="data.length == 0"
          >
            {{ this.$t("common.button.confirm") }}</el-button
          >
          <el-button
            type="primary"
            @click="handOutResult"
            v-if="activeName == 'singleResult'"
            :loading="loading"
            :disabled="data.length == 0"
            v-hasPermi="['monthplan:mpWeekRollAdjust:autoAdjust']"
          >
            {{ this.$t("common.button.confirm") }}</el-button
          >

		  <!-- <el-button
            @click="handleExport"
            v-hasPermi="['monthplan:factoryMonthPlanFinalResult:export']"
            >{{ $t("ui.frame.btn.export") }}</el-button
          >

          <el-button
            v-hasPermi="['monthplan:factoryMonthPlanFinalResult:import']"
            @click="$refs.tltUpload.handleImport()"
            >{{ $t("ui.frame.btn.import") }}</el-button
          >
          <el-button
            :loading="syncLoading"
            v-hasPermi="['monthplan:factoryMonthPlanFinalResult:sync']"
            @click="handleIssueScmMes"
            >{{
              $t("ui.data.column.monthPlanFinalAdjustQuery.issueScmMes")
            }}</el-button
          > -->

        </div>
      </template>
    </page-table>
    <!--
      结构调整单结构流程：getOutList「获取调整订单」后 showOutResult 为 true 时，原会在主表下方再展示结构明细 el-table。
      业务要求暂不展示该表：由 displayOutResultDetailTable 控制（默认 false）；需恢复时将 data 中该项改为 true。
    -->
    <!-- <div v-if="showOutResult">
      <el-table
        v-if="displayOutResultDetailTable"
        :data="outResultData"
        border
        style="width: 100%"
        :row-class-name="tableRowClassName"
        max-height="450"
      >
        <el-table-column
          v-for="item in outResultColumns"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
          :min-width="item.minWidth"
          :fixed="item.fixed ? true : false"
        >
          <template v-slot="scope" v-if="item.prop == 'isLockSchedule' || item.editable">
            <div v-if="item.prop == 'isLockSchedule'">
              <el-select
                v-if="scope.row.id"
                v-model="scope.row.isLockSchedule"
                @change="handleLockScheduleChange(scope.row, $event)"
                size="mini"
              >
                <el-option
                  v-for="option in dict.type.biz_yes_no"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <span v-else>{{
                selectDictLabel(dict.type.biz_yes_no, scope.row.isLockSchedule)
              }}</span>
            </div>
            <div v-else-if="item.editable && scope.row.id">
              <el-input
                :value="scope.row[item.prop] || ''"
                size="mini"
                @input="scope.row[item.prop] = $event.replace(/[^\d]/g, '')"
                @focus="onDayEditFocus(scope.row, item.prop)"
                @blur="handleOutResultDayEdit(scope.row, item.prop)"
              />
            </div>
            <span v-else>{{ scope.row[item.prop] || '' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div
        style="
          display: flex;
          flex-direction: row;
          align-items: center;
          justify-content: center;
        "
      >
        <el-button @click="backPlan">
          {{ this.$t("common.button.cancel") }}</el-button
        >
        <el-button
          type="primary"
          @click="confirmResult"
          :loading="loading"
          :disabled="data.length == 0"
        >
          {{ this.$t("common.button.confirm") }}</el-button
        >
      </div>
    </div> -->
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <el-dialog
      :title="$t('ui.data.column.monthPlanFinalAdjustQuery.issueScmMes')"
      :visible.sync="syncDialog.visible"
      width="520px"
      append-to-body
      @close="resetSyncDialog"
    >
      <el-form
        ref="syncForm"
        :model="syncDialog.form"
        :rules="syncDialogRules"
        label-width="120px"
      >
        <el-form-item
          :label="$t('ui.data.column.report.proSizeSummary.yearMonth')"
          prop="yearMonth"
        >
          <el-date-picker
            v-model="syncDialog.form.yearMonth"
            type="month"
            value-format="yyyy-MM"
            format="yyyy-MM"
            :placeholder="
              $t('ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleYearMonth')
            "
            style="width: 100%"
            @change="handleSyncBaseChange"
          />
        </el-form-item>
        <el-form-item :label="$t('common.factory')" prop="factoryCode">
          <el-select
            v-model="syncDialog.form.factoryCode"
            :placeholder="$t('ui.frame.btn.choose')"
            filterable
            clearable
            style="width: 100%"
            @change="handleSyncBaseChange"
          >
            <el-option
              v-for="dict in dict.type.biz_factory_name"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('ui.data.monthlyProductionPlan.productionVersion')"
          prop="productionVersion"
        >
          <el-select
            v-model="syncDialog.form.productionVersion"
            :placeholder="
              $t(
                'ui.data.column.monthPlanFinalAdjustQuery.issueScmMesPlaceholderProductionVersion'
              )
            "
            filterable
            clearable
            style="width: 100%"
            :loading="syncDialog.versionLoading"
            @change="handleSyncProductionVersionChange"
          >
            <el-option
              v-for="item in syncProductionVersionOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('ui.data.monthlyProductionPlan.lastMonthPlanVersion')"
          prop="lastMonthPlanVersion"
        >
          <el-select
            v-model="syncDialog.form.lastMonthPlanVersion"
            :placeholder="
              $t(
                'ui.data.column.monthPlanFinalAdjustQuery.issueScmMesPlaceholderDemandVersion'
              )
            "
            filterable
            clearable
            style="width: 100%"
            :loading="syncDialog.versionLoading"
            @change="handleSyncDemandVersionChange"
          >
            <el-option
              v-for="item in syncDemandVersionOptions"
              :key="item.optionKey"
              :label="item.lastMonthPlanVersion"
              :value="item.optionKey"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="syncDialog.visible = false">{{
          $t("common.button.cancel")
        }}</el-button>
        <el-button
          type="primary"
          :loading="syncLoading"
          @click="submitSyncAdjustedMonthPlan"
          >{{
            $t("ui.data.column.monthPlanFinalAdjustQuery.issueScmMesOk")
          }}</el-button
        >
      </span>
    </el-dialog>
    <tlt-upload
      ref="tltUpload"
      downloadUrl=""
      uploadUrl="/monthplan/factoryMonthPlanFinalResult/importSkuScheduleItems"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />

    <special ref="specialRef"></special>
    <addModal ref="addModalRef" @success="addSuccessFun" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapState, mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  getFinalResultVersionList,
  syncAdjustedMonthPlanToScmAndMes,
} from "@/api/monthplan/monthlyProductionPlan";
import {
  listInternalStructure,
  getAdjustDetailList,
  listOutsideStructure,
  confirmAdjust,
  autoAdjust,
  saveAdjust,
  removeAdjust,
  removeStructure,
  versionAdjust,
  versionStructure,
  getStructureDetail,
  listResult,
  resultVersion,
  listOutHistory,
  editOutHistory,
  removeOutHistory,
  versionOutHistory,
  outNextStructure,
  saveAdjustResult,
  statisticsResult,
  outGetStayDay,
  updateSkuScheduleItems,
} from "@/api/monthplan/adjustStructure";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import result from "./components/result.vue";
import special from "./components/special.vue";
import addModal from "./components/addModal.vue";

/** 与 monthlyProductionPlan/components/structureAdjustDialog.vue 一致：弹窗「选择」缓存整行 listAdjusts */
const MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY =
  "mpMonthPlanStructureAdjust.prefillListAdjustRow";

export default {
  name: "RollingCycle",
  props: {
    /**
     * 页面类型：结构内调整与结构调整拆分为两个路由，由外层包装组件传入。
     * structureInner — 原「结构内」Tab；structureAdjust — 原「结构调整」Tab（含单结构流程）。
     */
    pageVariant: {
      type: String,
      required: true,
      validator: (v) => ["structureInner", "structureAdjust"].includes(v),
    },
  },
  components: {
    tltUpload,
    infoDialog,
    result,
    special,
    addModal,
  },
  dicts: [
    "biz_yes_no",
    "biz_factory_name",
    "biz_machine_brand",
    "biz_class_type",
    "week_roll_adjust_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      subDayNum: 0,
      dayEditOriginalValue: null,
      loadText: "正在加载中...",
      //结构外调整结果列表
      outResultData: [],
      outResultVersion: "",
      /** 是否渲染单结构流程「获取调整订单」下方的结构明细表（7 列+锁定上机+1～31 号）；false 为隐藏 */
      displayOutResultDetailTable: false,
      showOutResult: false,
      nextLoading: false,
      showConfirmResult: false,

      isShowFoot: false,
      formInline: {},
      isTabChange: true, //从tab页点击到调整结果页
      versionList: [],
      isShowResult: false,
      getLoading: false,
      autoLoading: false,
      adjustType: "01",
      show: true,
      subLoading: false,
      activeName: "first",
      expands: [],
      tableData: [],
      subLoading: false,
      subTableData: [],
      loading: false,
      data: [],
      selection: [],
      page: null,
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
      isEdit: true,
      typeList: [
        { label: "结构内", value: "01" },
        { label: "结构调整", value: "02" },
      ],
      actionDate: {},
      /** 从月计划调整查询「选择」进入，用于取消时清理路由参数 */
      monthPlanFromFinalSelect: false,

      syncLoading: false,
      /** 下发 SCM/MES 弹窗（与月计划调整查询页同源接口；弹窗年月独立按 backup-legacy 默认下月初始化） */
      syncDialog: {
        visible: false,
        versionLoading: false,
        versionList: [],
        form: {
          yearMonth: "",
          factoryCode: "",
          productionVersion: "",
          lastMonthPlanVersion: "",
          monthPlanVersion: "",
        },
      },

      dayList: 31,
      // 调整结果页日计划量是否可编辑：仅自动调整后可编辑
      resultDayEditable: false,
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    /** 结构内调整独立页 */
    isStructureInnerPage() {
      return this.pageVariant === "structureInner";
    },
    /** 结构调整独立页 */
    isStructureAdjustPage() {
      return this.pageVariant === "structureAdjust";
    },
    columns() {
      if (!this.show) {
        return [];
      }
      if (this.activeName == "first") {
        return [
          {
            prop: "structureName",
            label: this.$t("产品结构"),
            width: 180,
            fixed: "left",
          },
          {
            prop: "scheduledMachines",
            label: this.$t("排产机台"),
            width: 120,
            fixed: "left",
          },
          {
            prop: "version",
            label: this.$t("版本号"),
            width: 150,
          },
          {
            prop: "materialCode",
            label: this.$t("物料编码"),
            width: 120,
            fixed: "left",
          },
          {
            prop: "materialDesc",
            label: this.$t("物料描述"),
          align: "left",
            minWidth: 350,
            fixed: "left",
          },
          {
            prop: "hasSpecialMaterial",
            label: this.$t("是否含特殊材料"),
            width: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            prop: "previousNetQty",
            label: this.$t("调整前净需求量（上周）"),
            width: 120,
          },
          {
            prop: "currentNetQty",
            label: this.$t("当前净需求量"),
            width: 120,
          },
          {
            prop: "netQtyChange",
            label: this.$t("净需求变动"),
            width: 120,
          },
          {
            prop: "monthScheduledQty",
            label: this.$t("月计划已排产量"),
            width: 120,
          },
          {
            prop: "productionQty",
            label: this.$t("月计划已生产量"),
            width: 120,
          },
          {
            prop: "pendingQty",
            label: this.$t("待调整量"),
            width: 120,
          },
          {
            prop: "confirmAdjustQty",
            label: this.$t("确认调整量"),

            render: ({ row }) => {
              return (
                <div>
                  <el-input
                    key={row.id}
                    v-model={row.confirmAdjustQty}
                    placeholder="请输入内容"
                    onInput={(value) => {
                      // 移除小数点和小数部分
                      const matched = value.match(/^-?\d*/);
                      row.confirmAdjustQty = matched ? matched[0] : '';
                      {/* row.confirmAdjustQty = value.replace(/\./g, ""); */}
                    }}
                    onBlur={(e) => {
                      e.preventDefault(); // 如果需要阻止默认行为
                      this.editAdjust(row);
                    }}
                    size="mini"
                  ></el-input>
                </div>
              );
            },
          },
          {
            prop: "adjustPriority",
            label: this.$t("调整优先级"),
            render: ({ row }) => {
              return (
                <div>
                  <el-input
                    key={row.id}
                    v-model={row.adjustPriority}
                    disabled={row.isSkuAdd != "1"}
                    onInput={(value) => {
                      // 移除小数点、负号和其他非数字字符
                      // ^[1-9]\d*$ 匹配正整数，不包括0
                      // 将值设置为正整数

                      row.adjustPriority = value.replace(/[^\d]/g, "");
                      if (value > 99999) {
                        row.adjustPriority = 99999;
                      }
                    }}
                    placeholder="请输入"
                    min={0}
                    onBlur={(e) => {
                      e.preventDefault(); // 如果需要阻止默认行为
                      this.editAdjust(row, "adjustPriority");
                    }}
                    size="mini"
                  ></el-input>
                </div>
              );
            },
          },
          {
            prop: "actualAdjustQty",
            label: this.$t("实际调整"),
            width: 120,
            render: ({ row }) => {
              return (
                <div
                  style={{ background: row.actualAdjustQty ? "yellow" : "" }}
                >
                  {row.actualAdjustQty}
                </div>
              );
            },
          },
          // {
          //   prop: "adjustmentReason",
          //   label: this.$t("调整原因"),
          //   width: 120,
          // },
          {
            align: "center",
            label: this.$t("ui.data.btn.option"),
            fixed: "right",
            render: ({ row }) => {
              return (
                <div>
                  <el-button
                    v-hasPermi={["monthplan:mpAdjustStructureIn:remove"]}
                    class="minus"
                    type="danger"
                    onClick={() => this.handleDelete(row)}
                  >
                    {this.$t("ui.frame.btn.delete")}
                  </el-button>
                </div>
              );
            },
          },
        ];
      }
      if (this.activeName == "second") {
        return [
          { type: "selection", fixed: "left" },
          {
            prop: "expand",
            type: "expand",
            render: () => {
              return (
                <div class="expend-table" v-loading={this.subLoading}>
                  <el-table border data={this.subTableData} max-height="200px">
                    {this.subColumns.map((item) => {
                      return (
                        <el-table-column
                          prop={item.prop}
                          label={item.label}
                          minWidth={item.width ? item.width : "100px"}
                          scopedSlots={{
                            default: item.render ? item.render : undefined,
                          }}
                        />
                      );
                    })}
                  </el-table>
                </div>
              );
            },
          },
          // {
          //   prop: "productionVersion",
          //   label: this.$t("版本号"),
          // },
          {
            prop: "cxMachineCode",
            label: this.$t("成型机台"),
          },
          {
            prop: "structureName",
            label: this.$t("产品结构"),
          },

          {
            prop: "beginDay",
            label: this.$t("开始日期"),
          },
          {
            prop: "endDay",
            label: this.$t("结束日期"),
          },
          {
            align: "center",
            label: this.$t("ui.data.btn.option"),
            fixed: "right",
            render: ({ row }) => {
              return (
                <div>
                  {/* <el-button
                    class="minus"
                    type="primary"
                    onClick={() => this.handleRowClick(row)}
                  >
                    {row.id == this.expands[0] ? "收起" : "展开"}
                  </el-button> */}
                  <el-button
                    class="minus"
                    v-hasPermi={["monthplan:mpStructureAllocation:remove"]}
                    type="danger"
                    disabled={row.dataSource != "01"}
                    onClick={() => this.handleStructureDelete(row)}
                  >
                    {this.$t("ui.frame.btn.delete")}
                  </el-button>
                </div>
              );
            },
          },
          // {
          //   prop: "beforePlanQty",
          //   label: this.$t("计划量"),
          // },
          // {
          //   prop: "afterPlanQty",
          //   label: this.$t("调整后计划量"),
          // },
          // {
          //   prop: "beforeEndDate",
          //   label: this.$t("调整后开始日期"),
          // },
          // {
          //   prop: "afterStartDate",
          //   label: this.$t("调整后结束日期"),
          // },
        ];
      }
      if (this.activeName == "three") {
        let list = [
          {
            prop: "cxMachineCode",
            label: this.$t("成型机台"),
            width: 120,
          },

          {
            prop: "structureName",
            label: this.$t("产品结构"),
            width: 180,
          },
          {
            prop: "materialCode",
            label: this.$t("物料编码"),
            width: 120,
          },
          {
            prop: "materialDesc",
            label: this.$t("物料描述"),
          align: "left",
            minWidth: 350,
          },
          {
            prop: "hasSpecialMaterial",
            label: this.$t("是否含特殊材料"),
            width: 120,
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            prop: "totalPlanQty",
            label: this.$t("计划量"),
            width: 120,
          },
          {
            prop: "beginDay",
            label: this.$t("开始日期"),
            width: 120,
          },
          {
            prop: "endDay",
            label: this.$t("结束日期"),
            width: 120,
          },
          {
            prop: "isLockSchedule",
            width: 120,
            label: this.$t("锁定上机日期"),
            render: ({ row }) => {
              return (
                <div>
                  {row.id && (
                    <el-select
                      v-model={row.isLockSchedule}
                      onChange={(val) =>
                        this.handleLockScheduleChange(row, val)
                      }
                    >
                      {this.dict.type.biz_yes_no.map((item) => (
                        <el-option
                          key={item.value}
                          label={item.label}
                          value={item.value}
                        ></el-option>
                      ))}
                    </el-select>
                  )}
                  {!row.id && (
                    <span>
                      {this.selectDictLabel(
                        this.dict.type.biz_yes_no,
                        row.isLockSchedule
                      )}
                    </span>
                  )}
                </div>
              );
            },
            // formatter: (row, column, value) => {
            //   return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            // },
          },
        ];
        if (this.isTabChange) {
          list.splice(1, 0, {
            prop: "version",
            label: this.$t("版本号"),
            width: 180,
          });
        }
        const days = 31;
        for (let i = 0; i < days; i++) {
          list.push({
            label: `${i + 1}号`,
            // label: this.$t("ui.data.column.mouldingDayResult.day", {
            //   day: i + 1,
            // }),
            prop: `day${i + 1}`,
            minWidth: "80px",
            type: "number",
            render: ({ row }) => {
              const prop = `day${i + 1}`;
              return (
                <div>
                  {row.id ? (
                    this.resultDayEditable ? (
                      <el-input
                        value={row[prop] || ""}
                        size="mini"
                        onInput={(value) => {
                          row[prop] = (value || "").replace(/[^\d]/g, "");
                        }}
                        onFocus={() => this.onDayEditFocus(row, prop)}
                        onBlur={() => this.handleResultDayEdit(row, prop)}
                      ></el-input>
                    ) : (
                      <span>{row[prop] || ""}</span>
                    )
                  ) : (
                    <span>{row[prop] || ""}</span>
                  )}
                </div>
              );
            },
          });
        }
        return list;
      }
      if (this.activeName == "singleResult") {
        return [
          {
            prop: "materialCode",
            label: this.$t("物料编码"),
            width: 120,
            fixed: "left",
          },
          {
            prop: "materialDesc",
            label: this.$t("物料描述"),
          align: "left",
            minWidth: 350,
            fixed: "left",
          },

          {
            prop: "structureName",
            label: this.$t("产品结构"),
            width: 180,
          },
          {
            prop: "version",
            label: this.$t("版本号"),
            width: 180,
          },

          {
            prop: "scheduledMachines",
            label: this.$t("排产机台"),
            width: 120,
          },

          {
            prop: "hasSpecialMaterial",
            label: this.$t("是否含特殊材料"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
            width: 120,
          },
          {
            prop: "previousNetQty",
            label: this.$t("调整前净需求量（上周）"),
            width: 120,
          },
          {
            prop: "currentNetQty",
            label: this.$t("当前净需求量"),
            width: 120,
          },
          {
            prop: "netQtyChange",
            label: this.$t("净需求变动"),
            width: 120,
          },
          {
            prop: "monthScheduledQty",
            label: this.$t("月计划已排产量"),
            width: 120,
          },
          {
            prop: "productionQty",
            label: this.$t("月计划已生产量"),
            width: 120,
          },
          {
            prop: "pendingQty",
            label: this.$t("待调整量"),
            width: 120,
          },
          {
            prop: "confirmAdjustQty",
            label: this.$t("确认调整量"),
            render: ({ row }) => {
              return (
                <div>
                  {this.isEdit && (
                    <el-input
                      key={row.id}
                      v-model={row.confirmAdjustQty}
                      placeholder="请输入内容"
                      onInput={(value) => {
                        // 移除小数点和小数部分
                        const matched = value.match(/^-?\d*/);
                        row.confirmAdjustQty = matched ? matched[0] : '';
                        {/* row.confirmAdjustQty = value.replace(/\./g, ""); */}
                      }}
                      onBlur={(e) => {
                        e.preventDefault(); // 如果需要阻止默认行为
                        this.editOutAdjust(row);
                      }}
                      size="mini"
                    ></el-input>
                  )}
                  {!this.isEdit && <span>{row.confirmAdjustQty}</span>}
                </div>
              );
            },
          },
          {
            prop: "adjustPriority",
            label: this.$t("调整优先级"),
            render: ({ row }) => {
              return (
                <div>
                  {this.isEdit && (
                    <el-input
                      key={row.id}
                      v-model={row.adjustPriority}
                      disabled={row.isSkuAdd != "1"}
                      placeholder="请输入"
                      min={0}
                      onInput={(value) => {
                        // 移除小数点、负号和其他非数字字符
                        // ^[1-9]\d*$ 匹配正整数，不包括0
                        // 将值设置为正整数
                        row.adjustPriority = value.replace(/[^\d]/g, "");
                        if (value > 99999) {
                          row.adjustPriority = 99999;
                        }
                      }}
                      onBlur={(e) => {
                        e.preventDefault(); // 如果需要阻止默认行为
                        this.editOutAdjust(row, "adjustPriority");
                      }}
                      size="mini"
                    ></el-input>
                  )}
                  {!this.isEdit && <span>{row.adjustPriority}</span>}
                </div>
              );
            },
          },
          {
            prop: "actualAdjustQty",
            label: this.$t("实际调整"),
            width: 120,
            render: ({ row }) => {
              return (
                <div
                  style={{ background: row.actualAdjustQty ? "yellow" : "" }}
                >
                  {row.actualAdjustQty}
                </div>
              );
            },
          },
          // {
          //   prop: "adjustmentReason",
          //   label: this.$t("调整原因"),
          //   width: 120,
          // },
          {
            align: "center",
            label: this.$t("ui.data.btn.option"),
            fixed: "right",
            render: ({ row }) => {
              return (
                <div>
                  <el-button
                    v-hasPermi={["monthplan:ProductMoldingLimit:remove"]}
                    class="minus"
                    type="danger"
                    onClick={() => this.handleOutDelete(row)}
                  >
                    {this.$t("ui.frame.btn.delete")}
                  </el-button>
                </div>
              );
            },
          },
        ];
      }

      return [];
    },
    searchColumns() {
      let list = [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
          listeners: {
            change: this.handleFactoryChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.handleYearMonthChange,
          },
        },
        // {
        //   prop: "scheduledMachines",
        //   label: this.$t("成型机台"),
        // },
      ];
      if(this.pageVariant === 'structureInner'){
        list.push({
          prop: "structureName",
          label: this.$t("产品结构"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        })
      }
      /** 月计划结构调整独立路由：不展示查询区「版本号」（定稿版本仍可由 getVersionList 写入 query 供列表接口使用） */
      if (!this.isStructureAdjustPage) {
        list.push({
          prop:
            this.activeName == "second" ? "productionVersion" : "version",
          label: this.$t("版本号"),
          type: "select",
          clearable: this.activeName == "first" ? false : true,
          filterable: true,
          dictData: this.versionList,
          listeners: {
            change: this.handleVersionChange,
          },
        });
      }
      list.push(
        {
          prop: "materialCode",
          label: this.$t("物料编码"),
          listeners: {
            input: this.handleMaterialCodeChange,
          },
        },
        {
          prop: "materialDesc",
          label: this.$t("物料描述"),
        }
      );
      if (this.activeName == "three") {
        list.push({
          prop: "adjustType",
          label: this.$t("类型"),
          type: "select",
          dictData: this.typeList,
        });
      }
      return list;
    },
    subColumns() {
      let list = [
        {
          label: this.$t("成型机台"),
          prop: "cxMachineCode",
          width: 120,
        },
        // {
        //   label: this.$t("产品结构"),
        //   prop: "structureName",
        //   width: 120,
        // },
        {
          label: this.$t("物料编码"),
          prop: "materialCode",
          width: 120,
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
          width: 320,
        },
        // {
        //   label: this.$t("是否含物料"),
        //   prop: "hasSpecialMateriaL",
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        {
          label: this.$t("计划量"),
          prop: "totalQty",
          width: 120,
        },
        {
          label: this.$t("开始日期"),
          prop: "beginDay",
          width: 120,
        },
        {
          label: this.$t("结束日期"),
          prop: "endDay",
          width: 120,
        },
      ];
      const days = this.subDayNum || 31;
      for (let i = 0; i < days; i++) {
        list.push({
          label: `${i + 1}号`,
          // label: this.$t("ui.data.column.mouldingDayResult.day", {
          //   day: i + 1,
          // }),
          prop: `day${i + 1}`,
          minWidth: "80px",
          type: "number",
        });
      }
      return list;
    },
    /**
     * 结构明细表列配置（仅当 displayOutResultDetailTable 为 true 时 el-table 会挂载）。
     * 顺序：物料/机台/结构/计划量/起止日 → 锁定上机 → 1～31 号日列（editable）。
     */
    outResultColumns() {
      let list = [
        {
          prop: "materialCode",
          label: this.$t("物料编码"),
          fixed: "flex",
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("物料描述"),
          align: "left",
          minWidth: 350,
          fixed: "flex",
        },

        {
          prop: "cxMachineCode",
          label: this.$t("成型机台"),
          width: 120,
        },

        {
          prop: "structureName",
          label: this.$t("产品结构"),
          width: 180,
        },
        // {
        //   prop: "version",
        //   label: this.$t("版本号"),
        //   width: 180,
        // },

        // {
        //   prop: "hasSpecialMaterial",
        //   label: this.$t("是否含特殊材料"),
        //   width: 120,
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },
        {
          prop: "totalQty",
          label: this.$t("计划量"),
          width: 120,
        },
        {
          prop: "beginDay",
          label: this.$t("开始日期"),
          width: 120,
        },
        {
          prop: "endDay",
          label: this.$t("结束日期"),
          width: 120,
        },
        {
          prop: "isLockSchedule",
          width: 120,
          label: this.$t("锁定上机日期"),

          // formatter: (row, column, value) => {
          //   return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          // },
        },
      ];
      // 日计划列：day1～day31，与模板中 item.editable 分支联动可编辑
      const days = 31;
      for (let i = 0; i < days; i++) {
        list.push({
          label: `${i + 1}号`,
          // label: this.$t("ui.data.column.mouldingDayResult.day", {
          //   day: i + 1,
          // }),
          prop: `day${i + 1}`,
          minWidth: "80px",
          type: "number",
          editable: true,
        });
      }
      return list;
    },
    syncProductionVersionOptions() {
      const versionSet = new Set();
      this.syncDialog.versionList.forEach((item) => {
        if (item.productionVersion) {
          versionSet.add(item.productionVersion);
        }
      });
      return Array.from(versionSet);
    },
    syncDemandVersionOptions() {
      return this.syncDialog.versionList.filter((item) => {
        return item.productionVersion === this.syncDialog.form.productionVersion;
      });
    },
    syncDialogRules() {
      const t = (k) => this.$t(k);
      return {
        yearMonth: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleYearMonth"
            ),
            trigger: "change",
          },
        ],
        factoryCode: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleFactory"
            ),
            trigger: "change",
          },
        ],
        productionVersion: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleProductionVersion"
            ),
            trigger: "change",
          },
        ],
        lastMonthPlanVersion: [
          {
            required: true,
            message: t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesRuleDemandVersion"
            ),
            trigger: "change",
          },
        ],
      };
    },
  },
  methods: {
    refreshSearch() {
      this.search = {
        factoryCode: this.search.factoryCode,
        yearMonth: this.search.yearMonth,
        productionVersion: this.search.productionVersion,
        version: this.search.version,
      };
      this.query = {
        factoryCode: this.search.factoryCode,
        yearMonth: this.search.yearMonth,
        productionVersion: this.search.productionVersion,
        version: this.search.version,
      };
      this.getList();
    },
    //查询是否有权限
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    /**
     * 月计划调整查询页（src/views/newPage/monthlyProductionPlan/index.vue）在路由表中的入口。
     * 与控制台「月计划调整查询」一致使用 path；组件 option `name: "MonthPlanFinalAdjustQuery"` 不是路由 name，
     * 使用 router.push({ name: "MonthPlanFinalAdjustQuery" }) 会无法匹配路由，易出现空白页。
     */
    getMonthPlanFinalAdjustQueryRoute(queryExtra = {}) {
      const query = { ...queryExtra };
      if (this.search.factoryCode) {
        query.factoryCode = this.search.factoryCode;
      }
      if (this.search.yearMonth) {
        query.yearMonth = this.search.yearMonth;
      }
      return {
        path: "/monthPlanManagement/console/monthlyProductionPlan",
        query,
      };
    },
    /**
     * 月计划结构调整独立页自动调整后，带回 monthlyProductionPlan/index.vue 的 query（与 created 读取字段一致）。
     * @param {Array} resultList autoAdjust 规范化后的行列表
     */
    buildMonthPlanQueryAfterStructureAutoAdjust(resultList) {
      const routeQ = this.$route.query || {};
      const fi =
        this.formInline && typeof this.formInline === "object"
          ? this.formInline
          : {};
      const row0 =
        resultList && resultList.length > 0 && resultList[0]
          ? resultList[0]
          : {};
      const factoryCode =
        this.search.factoryCode ||
        this.query.factoryCode ||
        fi.factoryCode ||
        row0.factoryCode;
      let yearMonthStr = "";
      const ym = this.search.yearMonth || this.query.yearMonth;
      if (ym != null && String(ym).trim() !== "") {
        yearMonthStr = String(ym).trim();
      } else if (fi.year != null && fi.month != null) {
        const m = Number(fi.month);
        yearMonthStr = `${fi.year}-${m < 10 ? "0" + m : m}`;
      } else if (row0.year != null && row0.month != null) {
        const m = Number(row0.month);
        yearMonthStr = `${row0.year}-${m < 10 ? "0" + m : m}`;
      }
      const productionVersion = (
        this.search.productionVersion ||
        this.query.productionVersion ||
        fi.productionVersion ||
        row0.productionVersion ||
        ""
      )
        .toString()
        .trim();
      const version = (
        row0.version ||
        fi.version ||
        this.query.version ||
        this.search.version ||
        ""
      )
        .toString()
        .trim();
      const structureName = (
        fi.structureName ||
        routeQ.structureName ||
        ""
      )
        .toString()
        .trim();
      const queryExtra = {};
      if (factoryCode != null && String(factoryCode).trim() !== "") {
        queryExtra.factoryCode = String(factoryCode).trim();
      }
      if (yearMonthStr) {
        queryExtra.yearMonth = yearMonthStr;
      }
      if (productionVersion) {
        queryExtra.productionVersion = productionVersion;
      }
      if (version) {
        queryExtra.version = version;
      }
      if (structureName) {
        queryExtra.structureName = structureName;
      }
      return queryExtra;
    },
    //修改锁定上机日期
    handleLockScheduleChange(row, val) {
      saveAdjustResult({
        id: row.id,
        isLockSchedule: row.isLockSchedule,
      })
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          // this.getList();
        })
        .catch((err) => {
          console.log(err);
        });
    },

    //记录编辑前的原始值
    onDayEditFocus(row, prop) {
      this.dayEditOriginalValue = row[prop];
    },

    //将日期列的值归一化：null/undefined/''/'0'/0 都视为空
    normalizeDayValue(val) {
      if (val == null || val === '' || val === 0 || val === '0') return '';
      return String(val);
    },

    //按后端规则本地重算开始/结束日期
    recalculateBeginEndDay(row) {
      if (!row) return;
      const monthStartDay = 1;
      const monthMaxDay = 31;
      let realBeginDay = monthMaxDay + 1;
      let realEndDay = 0;
      for (let i = monthStartDay; i <= monthMaxDay; i++) {
        const dayField = `day${i}`;
        const dayVal = Number(row[dayField] || 0);
        if (dayVal !== 0) {
          if (realBeginDay > i) {
            realBeginDay = i;
          }
          if (realEndDay < i) {
            realEndDay = i;
          }
        }
      }
      row.beginDay = realBeginDay === monthMaxDay + 1 ? 0 : realBeginDay;
      row.endDay = realEndDay;
    },

    //给调整结果列表回填 productTypeCode（源数据来自单结构列表 this.data）
    enrichProductTypeCode(list = []) {
      if (!Array.isArray(list) || list.length === 0) return list;
      if (!Array.isArray(this.data) || this.data.length === 0) return list;
      return list.map((item) => {
        if (!item || !item.id || item.productTypeCode) return item;
        const sourceItem = this.data.find(
          (row) =>
            row.materialCode === item.materialCode &&
            row.structureName === item.structureName &&
            row.productionVersion === item.productionVersion &&
            row.version === item.version
        );
        if (sourceItem && sourceItem.productTypeCode) {
          return {
            ...item,
            productTypeCode: sourceItem.productTypeCode,
            dayVulcanizationQty: sourceItem.dayVulcanizationQty
          };
        }
        return item;
      });
    },

    //修改结构每日计划量
    async handleOutResultDayEdit(row, prop) {
      if (!row.id) return;
      const sourceItem = this.data.find(item => item.materialCode === row.materialCode);
      if (sourceItem && sourceItem.productTypeCode) {
        row.productTypeCode = sourceItem.productTypeCode;
      }
      console.log('row', row);
      console.log('this.data',this.data)
      const oldVal = this.normalizeDayValue(this.dayEditOriginalValue);
      const newVal = this.normalizeDayValue(row[prop]);
      if (newVal === oldVal) return;
      try {
        this.recalculateBeginEndDay(row);
        console.log("发送请求数据:", JSON.stringify(row));
        await saveAdjustResult(row);
        // this.getOutResultList(row.productionVersion, row.version);
      } catch (err) {
        console.log(err);
      }
    },

    //修改调整结果tab每日计划量
    async handleResultDayEdit(row, prop) {
      if (!row.id) return;
      const oldVal = this.normalizeDayValue(this.dayEditOriginalValue);
      const newVal = this.normalizeDayValue(row[prop]);
      if (newVal === oldVal) return;
      try {
        this.recalculateBeginEndDay(row);
        await saveAdjustResult(row);
        // 保持表格就地更新：接口成功后不重新拉取列表，避免闪动
      } catch (err) {
        console.log(err);
      }
    },

    //单结构调整提交
    onSubmit() {},

    addSuccessFun() {
      this.$set(this.page, "current", 1);
      // this.$set(this.search, "productionVersion", "");
      // this.$set(this.query, "productionVersion", "");
      this.getVersionList(true);
      // this.getList();
    },
    handleRowClick(row) {
      if (this.expands.includes(row.id)) {
        this.expands = [];
      } else {
        this.expands = []; //添加该代码实现手风琴模式，删除该代码取消手风琴模式
        this.expands.push(row.id);
        this.getSubList(row);
      }
    },
    isNoPositiveInteger(num) {
      return /^-?\d+$/.test(num);
    },
    isPositiveInteger(num) {
      return /^(0|[1-9]\d*)$/.test(num);
    },
    //判断是否为奇数
    isEven(number) {
      return number % 2 === 0;
    },

    async editAdjust(row, type) {
      // if (!type) {
      //   if (!this.isEven(row.confirmAdjustQty)) {
      //     return this.$modal.msgWarning(
      //       `物料编码${row.materialCode}--确认调整量不能为奇数`
      //     );
      //   }
      // }

      try {
        let res = await saveAdjust(row);
        // this.$modal.msgSuccess(res.msg);
        this.getList();
      } catch (err) {}
    },
    async editOutAdjust(row, type) {
      // if (!type) {
      //   if (!this.isNoPositiveInteger(row.confirmAdjustQty)) {
      //     return this.$modal.msgWarning("不能有小数点");
      //   }
      // } else {
      //   if (!this.isPositiveInteger(row.adjustPriority)) {
      //     return this.$modal.msgWarning("请输入正整数");
      //   }
      // }
      // if (!type) {
      //   if (!this.isEven(row.confirmAdjustQty)) {
      //     return this.$modal.msgWarning(
      //       `物料编码${row.materialCode}--确认调整量不能为奇数`
      //     );
      //   }
      // }
      try {
        let res = await editOutHistory(row);
        // this.$modal.msgSuccess(res.msg);
        this.getSingleList({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          version: row.version,
          productionVersion: row.productionVersion,
        });
      } catch (err) {}
    },

    //单结构调整编辑后重新获取列表
    async getSingleList(params) {
      try {
        let res = await listOutHistory(params);
        this.data = res.rows;
      } catch (err) {
      } finally {
        this.subLoading = false;
      }
    },
    handleVersionChange(val) {
      if (this.activeName == "second") {
        this.search = {
          ...this.search,
          productionVersion: val,
        };
        this.query = {
          ...this.search,
          productionVersion: val,
        };
      } else {
        this.search = {
          ...this.search,
          version: val,
        };
        this.query = {
          ...this.search,
          version: val,
        };
      }
    },

    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        yearMonth: val,
      };
      this.query = {
        ...this.search,
        yearMonth: val,
      };

      this.getVersionList(true);
    },
    handleMaterialCodeChange(val) {
      this.search = {
        ...this.search,
        materialCode: val,
      };
      this.query = {
        ...this.search,
        materialCode: val,
      };
      // this.$set(this.search,'materialCode',val)
      // this.$set(this.query,'materialCode',val)
    },
    handleFactoryChange(val) {
      this.search = {
        ...this.search,
        factoryCode: val,
      };
      this.query = {
        ...this.search,
        factoryCode: val,
      };

      this.getVersionList();
    },
    //获取版本列表
    async getVersionList(isGet = false, isNewVersion = false) {
      this.loading = true;
      let res;
      try {
        if (this.activeName == "first") {
          res = await versionAdjust(this.formatParams());
        } else if (this.activeName == "second") {
          res = await versionStructure(this.formatParams());
        } else if (this.activeName == "three") {
          if (!this.isTabChange) return;
          res = await resultVersion(this.formatParams());
        } else {
          if (isGet) {
            this.getList();
          } else {
            this.loading = false;
          }
          return;
        }

        let list = [];
        for (let i = 0; i < res.rows.length; i++) {
          let obj = {
            label:
              this.activeName == "second"
                ? res.rows[i].productionVersion
                : res.rows[i].version,
            value:
              this.activeName == "second"
                ? res.rows[i].productionVersion
                : res.rows[i].version,
          };
          list.push(obj);
        }

        this.versionList = list;

        if (list.length > 0) {
          if (this.activeName == "second") {
            this.$set(this.search, "productionVersion", list[0].value);
            this.$set(this.query, "productionVersion", list[0].value);
          } else {
            if (isNewVersion) {
              this.$set(this.search, "version", list[0].value);
              this.$set(this.query, "version", list[0].value);
              return;
            }
            if (this.query.version) {
              let hasVersion = list.some(
                (item) => item.value == this.query.version
              );
              if (hasVersion) {
                this.$set(this.search, "version", this.query.version);
                this.$set(this.query, "version", this.query.version);
                return;
              }
            }
            this.$set(this.search, "version", list[0].value);
            this.$set(this.query, "version", list[0].value);
          }
        } else {
          this.$set(this.search, "version", "");
          this.$set(this.query, "version", "");
          this.$set(this.search, "adjVersion", "");
          this.$set(this.query, "adjVersion", "");
          this.$set(this.search, "productionVersion", "");
          this.$set(this.query, "productionVersion", "");
          // if (this.activeName != "second") {
          //   this.$set(this.search, "version", "");
          //   this.$set(this.query, "version", "");
          // } else if (this.activeName == "four") {
          //   this.$set(this.search, "adjVersion", "");
          //   this.$set(this.query, "adjVersion", "");
          // } else {
          //   this.$set(this.search, "productionVersion", "");
          //   this.$set(this.query, "productionVersion", "");
          // }
        }
      } catch (err) {
        console.log(err);
      } finally {
        if (isGet) {
          this.getList();
        } else {
          this.loading = false;
        }
      }
    },

    //获取单选历史列表的版本号
    async getOutVersionList(isGet) {
      try {
        const params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
        }
        let res = await versionOutHistory(params);
        let list = [];
        for (let i = 0; i < res.rows.length; i++) {
          let obj = {
            label: res.rows[i].version,
            value: res.rows[i].version,
          };
          list.push(obj);
        }

        this.versionList = list;
        // if (list.length > 0) {
        //   this.$set(this.search, "version", list[0].value);
        //   this.$set(this.query, "version", list[0].value);
        // } else {
        //   this.$set(this.search, "version", "");
        //   this.$set(this.query, "version", "");
        // }
        if (isGet) {
          this.$nextTick(() => {
            this.getOutHistoryList();
          });
        } else {
          this.$set(this.search, "version", "");
          this.$set(this.query, "version", "");
        }
      } catch (err) {
        console.log(err);
      } finally {
        this.page = null;
        this.show = true;
        this.loading = false;
        this.isShowResult = true;
        this.activeName = "singleResult";
      }
    },

    backPlan() {
      if (
        this.pageVariant === "structureAdjust" &&
        this.monthPlanFromFinalSelect
      ) {
        this.monthPlanFromFinalSelect = false;
        this.$router.push(this.getMonthPlanFinalAdjustQueryRoute());
      }
      this.show = false;
      this.showConfirmResult = false;
      this.resultDayEditable = false;
      if (this.adjustType == "01") {
        this.activeName = "first";
      } else {
        this.activeName = "second";
        this.page = {
          current: 1,
          pageSize: 20,
          total: 0,
        };
      }
      this.isShowFoot = false;
      this.isShowResult = false;
      this.showOutResult = false;
      this.isTabChange = true;
      this.getVersionList(true);
    },
    //确认调整结果
    async confirmResult() {
      this.loadText = this.$t("正在加载中，请稍候");
      try {
        this.show = false;
        this.loading = true;
        let params = {
          ...this.query,
          ...this.sort,
          adjustType: this.adjustType,
          version:
            this.adjustType == "01"
              ? this.data[0]?.version
              : this.outResultData[0]?.version,
          productionVersion: this.data[0]?.productionVersion,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        params.startDay = this.formInline.beginDay;
        params.endDay = this.formInline.endDay;
        params.adjustEndDay = this.formInline.adjustEndDay;
        params.adjustStartDay = this.formInline.adjustStartDay;
        params.structureName = this.formInline.structureName;
        params.scheduledMachines = this.formInline.cxMachineCode;
        // if (this.adjustType == "02") {
        //   params.adjustEndDay = this.formInline.adjustEndDay;
        //   params.isMove = this.formInline.isMove;
        // }
        let res = await confirmAdjust(params);

        this.$modal.msgSuccess(res.msg);
        if (this.activeName == "singleResult") {
          this.showOutResult = true;
          this.isShowFoot = false;
          this.isEdit = false;
          this.data = [];
          this.outResultData = [];
          this.loading = false;
        } else {
          this.show = false;
          this.backPlan();
        }
      } catch (err) {
        this.loading = false;
        this.show = true;
      }
    },
    //获取调整订单
    async adjustOrder() {
      this.loadText = this.$t("正在获取调整订单，请稍候");
      try {
        this.loading = true;
        this.getLoading = true;
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        // if (this.activeName == "first") {
        //   params.adjustType = "01";
        // } else {
        //   params.adjustType = "02";
        // }
        params.adjustType = this.adjustType;
        this.isEdit = true;
        let res = await getAdjustDetailList(params);
        if (res.rows) {
          this.data = res.rows;
        }
        this.getLoading = false;
        this.getVersionList(false, true);
        this.loading = false;
      } catch (err) {
        this.getLoading = false;
        this.loading = false;
      }
    },
    getDaysInMonth(year, month) {
      // 注意：JavaScript中月份从0开始，所以需要将传入的月份减1
      // 将日期设置为下个月的第0天，即当前月的最后一天
      return new Date(year, month, 0).getDate();
    },
    async getSubList(row) {
      this.subTableData = [];
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = "";
        }
        params.structureName = row.structureName;
        params.productionVersion = row.productionVersion;
        let res = await getStructureDetail(params);
        this.subTableData = res.rows;
      } catch (err) {}
    },
    async handleExpandChange(row, expandedRows) {
      this.expands = [];
      //通过当前的行获取
      if (expandedRows.length > 0) {
        this.subTableData = [];
        // try {
        //   let params = {
        //     ...this.query,
        //     ...this.sort,
        //   };
        //   if (params.yearMonth) {
        //     let arr = params.yearMonth.split("-");
        //     params.year = arr[0];
        //     params.month = arr[1];
        //     params.yearMonth = "";
        //   }
        //   params.structureName = row.structureName;
        //   params.productionVersion = row.productionVersion;
        //   let res = await getStructureDetail(params);

        //   this.subTableData = res.rows;
        // } catch (err) {}
        this.subDayNum = this.getDaysInMonth(row.year, row.month);
        this.getSubList(row);

        console.log();
        this.expands.push(row ? row.id : []);
        console.log("展开");
        // this.getSubList(row.id);
      } else {
        console.log("收起");
      }
    },
    handleAddSpecial() {
      if (this.$refs.addModalRef) {
        this.$refs.addModalRef.show({ yearMonth: this.search.yearMonth });
      }
    },

    //获取调整结果列表
    async getResultList() {
      try {
        let res = await listResult(this.formatParams());
        this.show = true;
        this.data = res.rows;
        console.log(res);
      } catch (err) {
        console.log(err);
      }
    },

    /**
     * 结构外（adjustType=02）autoAdjust 入参：query + 排序 + listAdjusts 整行 formInline + 订单首行版本字段，
     * 与 BootUI 示例一致（含 id、monthPlanVersion、adjustStartDay/EndDay、startDay、scheduledMachines、adjVersion 等）。
     */
    buildAutoAdjustParamsForStructureOuter() {
      const row0 =
        this.data && this.data.length > 0 && this.data[0]
          ? this.data[0]
          : {};
      const base =
        this.formInline && typeof this.formInline === "object"
          ? { ...this.formInline }
          : {};
      delete base._isNew;
      delete base._tmpId;
      let params = {
        ...this.query,
        ...this.sort,
        ...base,
        adjustType: this.adjustType,
      };
      /** 调整订单首行的 ADJ 版本优先；无则保留 formInline/query 上的 version */
      if (row0.version != null && String(row0.version).trim() !== "") {
        params.version = String(row0.version).trim();
      }
      if (params.yearMonth) {
        const arr = String(params.yearMonth).split("-");
        params.mpYear = arr[0];
        params.mpMonth = arr[1];
        params.yearMonth = "";
      }
      params.startDay = params.beginDay;
      params.scheduledMachines = params.cxMachineCode;
      const orderVer =
        row0.version != null && String(row0.version).trim() !== ""
          ? String(row0.version).trim()
          : "";
      if (
        (params.adjVersion == null || String(params.adjVersion).trim() === "") &&
        orderVer
      ) {
        params.adjVersion = orderVer;
      }
      const orderPv =
        row0.productionVersion != null &&
        String(row0.productionVersion).trim() !== ""
          ? String(row0.productionVersion).trim()
          : "";
      if (
        params.productionVersion == null ||
        String(params.productionVersion).trim() === ""
      ) {
        if (orderPv) {
          params.productionVersion = orderPv;
        }
      }
      return params;
    },

    /**
     * autoAdjust 经 request 返回可能是数组，或 { rows: [] } / { data: [] }；PageTable 的 data 必须为数组。
     */
    normalizeAutoAdjustResponse(raw) {
      if (Array.isArray(raw)) {
        return raw;
      }
      if (raw && Array.isArray(raw.rows)) {
        return raw.rows;
      }
      if (raw && Array.isArray(raw.data)) {
        return raw.data;
      }
      if (raw != null && typeof raw === "object") {
        console.warn("autoAdjust 返回非列表结构，已降级为空数组", raw);
      }
      return [];
    },

    //结构自动调整
    async handShowResult() {
      this.loadText = this.$t("正在自动调整，请稍候");
      this.show = false;
      this.loading = true;
      this.autoLoading = true;
      try {
        let params;
        if (this.adjustType === "02") {
          params = this.buildAutoAdjustParamsForStructureOuter();
        } else {
          params = {
            ...this.query,
            ...this.sort,
            ...(this.formInline && typeof this.formInline === "object"
              ? { ...this.formInline }
              : {}),
            adjustType: this.adjustType,
            version: this.data[0]?.version,
          };
          if (params.yearMonth) {
            let arr = params.yearMonth.split("-");
            params.mpYear = arr[0];
            params.mpMonth = arr[1];
            params.yearMonth = "";
          }
        }
        const raw = await autoAdjust(params);
        const list = this.normalizeAutoAdjustResponse(raw);
        console.log("autoAdjust", raw, list);
        /** 结构内独立页：closeOpenPage 之前勿改 data/show，避免表格随 list 重绘闪屏 */
        if (this.pageVariant === "structureInner") {
          const queryExtra = {};
          const structureName =
            this.formInline && this.formInline.structureName != null
              ? String(this.formInline.structureName).trim()
              : "";
          if (structureName) {
            queryExtra.structureName = structureName;
          }
          const versionProp =
            this.activeName === "second" ? "productionVersion" : "version";
          const versionVal = this.search[versionProp] ?? this.query[versionProp];
          if (
            versionVal != null &&
            String(versionVal).trim() !== ""
          ) {
            queryExtra[versionProp] = String(versionVal).trim();
          }
          this.$tab.closeOpenPage(
            this.getMonthPlanFinalAdjustQueryRoute(queryExtra)
          );
          this.loading = false;
          this.autoLoading = false;
          return;
        }
        this.data = list;
        this.show = true;
        this.loading = false;
        this.autoLoading = false;
        /** 月计划结构调整独立路由：自动调整成功后关当前页并回到月计划调整查询，携带工厂/年月/版本/结构等 */
        if (this.pageVariant === "structureAdjust") {
          const queryExtra =
            this.buildMonthPlanQueryAfterStructureAutoAdjust(list);
          this.$tab.closeOpenPage(
            this.getMonthPlanFinalAdjustQueryRoute(queryExtra)
          );
          return;
        }
        if (list.length != 0) {
          this.getStatisticsResult(list[0], 1);
        }
        this.isTabChange = false;
        this.isShowFoot = true;
        this.activeName = "three";
        this.resultDayEditable = true;
        this.versionList = [];
        this.$set(this.search, "version", "");
        this.$set(this.query, "version", "");
      } catch (err) {
        console.log(err);
        this.show = true;
        this.loading = false;
        this.autoLoading = false;
      }
    },

    //结构外自动调整
    async handOutResult() {
      this.loadText = this.$t("正在自动调整中，请稍候");
      for (let i = 0; i < this.data.length; i++) {
        if (
          this.data[i].confirmAdjustQty &&
          !this.isNoPositiveInteger(this.data[i].confirmAdjustQty)
        ) {
          return this.$modal.msgWarning(
            this.data[i].materialCode + "的调整量错误"
          );
        }
        if (
          this.data[i].adjustPriority &&
          !this.isPositiveInteger(this.data[i].adjustPriority)
        ) {
          return this.$modal.msgWarning(
            this.data[i].materialCode + "的优先级错误"
          );
        }
      }
      if (
        this.formInline.adjustEndDay == null ||
        this.formInline.adjustEndDay == ""
      ) {
        return this.$modal.msgWarning("请选择调整结束日期");
      }
      this.loading = true;
      try {
        let params = this.buildAutoAdjustParamsForStructureOuter();
        console.log("params", params);
        const raw = await autoAdjust(params);
        const list = this.normalizeAutoAdjustResponse(raw);
        this.outResultData = this.enrichProductTypeCode(list);
        if (this.pageVariant === "structureAdjust") {
          const queryExtra =
            this.buildMonthPlanQueryAfterStructureAutoAdjust(list);
          this.$tab.closeOpenPage(
            this.getMonthPlanFinalAdjustQueryRoute(queryExtra)
          );
          return;
        }
        if (list.length !== 0) {
          this.getStatisticsResult(list[0], 1);
          this.getSingleList({
            factoryCode: list[0].factoryCode,
            year: list[0].year,
            month: list[0].month,
            version: list[0].version,
            productionVersion: list[0].productionVersion,
          });
        }
        this.showConfirmResult = true;

        // this.data = res;
        // // this.data=res.rows
        // this.show = true;
        // this.loading = false;
        // this.autoLoading = false;
        // this.isTabChange = false;
        // this.isShowFoot = true;
        // this.activeName = "three";
      } catch (err) {
        console.log(err);
        this.show = true;
        this.loading = false;
      } finally {
        this.loading = false;
      }
    },

    handleShowSpecial() {
      if (this.$refs.specialRef) {
        this.$refs.specialRef.show();
      }
    },

    //单选结构调整
    async handleAdd() {
      this.formInline = this.selection[0];
      this.actionDate = this.selection[0];
      this.show = false;
      this.loading = true;
      this.isEdit = false;
      this.data = [];
      this.getStartDay(this.selection[0]);
      if (this.selection[0].productionVersion) {
        console.log("selection", this.selection[0]);
        // this.cxMachineCodeList(this.selection[0]);
        this.getOutVersionList();
      } else {
        this.page = null;
        setTimeout(() => {
          this.page = null;
          this.data = [];
          this.show = true;
          this.loading = false;
          this.isShowResult = true;
          this.activeName = "singleResult";
        }, 500);
      }
    },
    async cxMachineCodeList(row) {
      try {
        let list = [];
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = "";
        }
        params.structureName = row.structureName;
        params.productionVersion = row.productionVersion;
        let res = await getStructureDetail(params);

        for (let index = 0; index < res.rows.length; index++) {
          list.push(res.rows[index].cxMachineCode);
        }
        const uniqueArr = [...new Set(list)];
        let result = uniqueArr.join(", ");
        this.getOutHistoryList(result);
      } catch (err) {
        this.loading = false;
      }
    },
    //获取结构外调整历史列表
    async getOutHistoryList() {
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        // params.scheduledMachines = this.actionDate.cxMachineCode;
        // params.structureName = this.actionDate.structureName;

        params.adjustType = this.adjustType;
        this.isEdit = false;
        let res = await listOutHistory(params);
        this.page = null;
        this.data = res.rows;
        this.isShowResult = true;
        this.activeName = "singleResult";
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
        this.show = true;
      }
    },
    //刷新结构外调整历史列表
    async resizeOutHistoryList() {
      this.loading = true;
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        // params.scheduledMachines = this.actionDate.cxMachineCode;
        // params.structureName = this.actionDate.structureName;

        params.adjustType = this.adjustType;
        let res = await listOutHistory(params);
        this.page = null;
        this.data = res.rows;
        this.isEdit = false;
        this.showOutResult = false;
        this.isShowFoot = false;
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
        this.show = true;
      }
    },

    //结构外获取调整订单
    async getOutList() {
      this.loading = true;
      this.loadText = this.$t("正在获取调整订单，请稍候");
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }

        params.scheduledMachines = this.formInline.cxMachineCode;
        params.structureName = this.formInline.structureName;
        /** 月计划弹窗进入时 productionVersion 常在 formInline；getOutList 原只带 query，此处与周程 Tab 默认写入 query 的行为对齐 */
        const qPv =
          params.productionVersion != null
            ? String(params.productionVersion).trim()
            : "";
        const fPv =
          this.formInline &&
          this.formInline.productionVersion != null &&
          String(this.formInline.productionVersion).trim() !== ""
            ? String(this.formInline.productionVersion).trim()
            : "";
        if (!qPv && fPv) {
          params.productionVersion = fPv;
        }
        /** listAdjusts 行上的调整版本（version），周程勾选行进 formInline；获取订单时需带上 */
        const fVer =
          this.formInline &&
          this.formInline.version != null &&
          String(this.formInline.version).trim() !== ""
            ? String(this.formInline.version).trim()
            : "";
        const qpVer =
          params.version != null && String(params.version).trim() !== ""
            ? String(params.version).trim()
            : "";
        if (!qpVer && fVer) {
          params.version = fVer;
        }

        params.adjustType = this.adjustType;
        this.outResultData = [];
        this.isEdit = true;
        let res = await getAdjustDetailList(params);

        this.data = res.rows;
        this.getOutResultList(
          res.rows[0].productionVersion,
          res.rows[0].version
        );
        this.isShowFoot = true;
        this.showOutResult = true;
        if (!this.formInline.adjustStartDay) {
          this.formInline.adjustStartDay = this.formInline.beginDay;
        }
        if (!this.formInline.adjustEndDay) {
          this.formInline.adjustEndDay = this.formInline.endDay;
        }

        this.getOutVersionList();
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
      }
    },

    //结构外初始化结构列表
    async getOutResultList(productionVersion, version) {
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = "";
        }
        params.productionVersion = productionVersion;
        params.version = version;
        params.structureName = this.formInline.structureName;
        let res = await getStructureDetail(params);
        console.log("初始化结果");
        this.outResultData = this.enrichProductTypeCode(res.rows);
        if (res.rows.length != 0) {
          console.log("开始调用统计");
          this.getStatisticsResult(res.rows[0]);
        }
      } catch (err) {
        console.log(err);
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeAdjust({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.getList();
        });
      });
    },
    handleOutDelete(row, index) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;

        removeOutHistory({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.data = this.data.filter((item) => item.id != row.id);
          this.getSingleList({
            factoryCode: row.factoryCode,
            year: row.year,
            month: row.month,
            version: row.version,
            productionVersion: row.productionVersion,
          });
          // this.resizeOutHistoryList();
        });
      });
    },
    handleStructureDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeStructure({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.getList();
        });
      });
    },

    handleSearch(data) {
      this.query = data;
      console.log('this.activeName', this.activeName)
      if (this.activeName == "second") {
        this.$set(this.page, "current", 1);
      }
      if (this.activeName == "singleResult") {
        /** 与 index.backup-legacy 一致：单结构调整不重拉历史，避免 isEdit 被置 false 导致确认调整量/调整优先级不可编辑 */
        return;
      }
      this.loadText = this.$t("正在加载中，请稍候");
      
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      console.log(rows);
      this.selection = rows;
    },
	// 结构间调整导出
    handleExport() {
	  const params = this.formatParams(false);
	  params.structureName = this.formInline.structureName; // 只导出指定结构的数据
      downloadLink("/monthplan/factoryMonthPlanFinalResult/exportSkuScheduleItems", params);
    },

    /**
     * 与 monthlyProductionPlan/index.backup-legacy.vue created 中 defaultParams.yearMonth 一致：下月 `${year}-${month}`。
     * 仅用于下发弹窗预填，不改变周程页列表查询条件。
     */
    getYearMonthStringLikeBackupLegacyDefault() {
      const now = new Date();
      const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const y = nextMonth.getFullYear();
      const mo = nextMonth.getMonth() + 1;
      return `${y}-${mo}`;
    },
    /** 打开下发弹窗：年月按 backup-legacy 默认下月；分厂预填当前查询 */
    handleIssueScmMes() {
      this.syncDialog.visible = true;
      this.syncDialog.form.yearMonth = this.formatYearMonthForPicker(
        this.getYearMonthStringLikeBackupLegacyDefault()
      );
      this.syncDialog.form.factoryCode =
        this.query.factoryCode || this.search.factoryCode || "";
      this.syncDialog.form.productionVersion = "";
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
      this.syncDialog.versionList = [];
      this.$nextTick(() => {
        if (this.$refs.syncForm) {
          this.$refs.syncForm.clearValidate();
        }
      });
      this.loadSyncVersionList(true);
    },
    handleSyncBaseChange() {
      this.syncDialog.form.productionVersion = "";
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
      this.syncDialog.versionList = [];
      this.loadSyncVersionList(false);
    },
    handleSyncProductionVersionChange() {
      this.syncDialog.form.lastMonthPlanVersion = "";
      this.syncDialog.form.monthPlanVersion = "";
    },
    handleSyncDemandVersionChange(optionKey) {
      const selectedVersion = this.syncDialog.versionList.find(
        (item) => item.optionKey === optionKey
      );
      this.syncDialog.form.monthPlanVersion = selectedVersion
        ? selectedVersion.monthPlanVersion
        : "";
    },
    resetSyncDialog() {
      this.syncDialog.form = {
        yearMonth: "",
        factoryCode: "",
        productionVersion: "",
        lastMonthPlanVersion: "",
        monthPlanVersion: "",
      };
      this.syncDialog.versionList = [];
    },
    async loadSyncVersionList(showWarning) {
      const { yearMonth, factoryCode } = this.syncDialog.form;
      if (!yearMonth || !factoryCode) {
        return;
      }
      const yearMonthInfo = this.parseYearMonthFromStr(yearMonth);
      if (!yearMonthInfo) {
        return;
      }
      try {
        this.syncDialog.versionLoading = true;
        const res = await getFinalResultVersionList({
          factoryCode,
          year: yearMonthInfo.year,
          month: yearMonthInfo.month,
        });
        const rows = res.rows || [];
        this.syncDialog.versionList = rows
          .filter((item) => {
            return (
              item.productionVersion &&
              item.monthPlanVersion &&
              item.lastMonthPlanVersion
            );
          })
          .map((item) => {
            return {
              ...item,
              optionKey: `${item.productionVersion}__${item.monthPlanVersion}__${item.lastMonthPlanVersion}`,
            };
          });
        if (showWarning && this.syncDialog.versionList.length === 0) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesNoData"
            )
          );
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.syncDialog.versionLoading = false;
      }
    },
    submitSyncAdjustedMonthPlan() {
      this.$refs.syncForm.validate((valid) => {
        if (!valid) {
          return;
        }
        const selectedVersion = this.syncDialog.versionList.find((item) => {
          return item.optionKey === this.syncDialog.form.lastMonthPlanVersion;
        });
        if (!selectedVersion) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesNoData"
            )
          );
          return;
        }
        const yearMonthInfo = this.parseYearMonthFromStr(
          this.syncDialog.form.yearMonth
        );
        if (!yearMonthInfo) {
          this.$modal.msgWarning(
            this.$t(
              "ui.data.column.monthPlanFinalAdjustQuery.issueScmMesInvalidYearMonth"
            )
          );
          return;
        }
        const params = {
          factoryCode: this.syncDialog.form.factoryCode,
          year: yearMonthInfo.year,
          month: yearMonthInfo.month,
          monthPlanVersion: selectedVersion.monthPlanVersion,
          lastMonthPlanVersion: selectedVersion.lastMonthPlanVersion,
          productionVersion: this.syncDialog.form.productionVersion,
        };
        this.$confirm(
          this.$t(
            "ui.data.column.monthPlanFinalAdjustQuery.confirmPushAdjustedMonthPlanToScmMes"
          ),
          {
            type: "warning",
          }
        ).then(async () => {
          try {
            this.syncLoading = true;
            const res = await syncAdjustedMonthPlanToScmAndMes(params);
            this.$modal.msgSuccess(res.msg);
            this.syncDialog.visible = false;
            await this.getList();
          } catch (error) {
            console.error(error);
          } finally {
            this.syncLoading = false;
          }
        });
      });
    },
    formatYearMonthForPicker(yearMonth) {
      if (!yearMonth) {
        return "";
      }
      const m = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      return m.isValid() ? m.format("YYYY-MM") : "";
    },
    parseYearMonthFromStr(yearMonth) {
      if (!yearMonth) {
        return null;
      }
      const m = moment(yearMonth, ["YYYY-MM", "YYYY-M"], true);
      if (!m.isValid()) {
        return null;
      }
      return {
        year: m.year(),
        month: m.month() + 1,
      };
    },

    formatParams() {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (this.activeName == "second") {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = "";
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        console.log('this.activeName', this.activeName)
        console.log('this.activeName', this.activeName)
        let data;
        if (this.activeName == "first") {
          data = await listInternalStructure(this.formatParams());
        } else if (this.activeName == "second") {
          data = await listOutsideStructure(this.formatParams());
        } else if (this.activeName == "three") {
          if (!this.isTabChange) {
            return;
          }
          data = await listResult(this.formatParams());
          if (data.rows.length != 0) {
            this.getStatisticsResult(data.rows[0]);
          }
        } else {
          return;
        }

        this.data = data.rows;
        if (this.activeName == "second") {
          this.page.total = data.total;
        }

        this.show = true;
      } catch (error) {
        console.error(error);
        this.data = [];
      } finally {
        this.loading = false;
        this.show = true;
      }
    },

    //结构外下一个结构
    async nextStructure() {
      this.nextLoading = true;
      try {
        let params = {
          ...this.query,
          ...this.sort,
          ...this.formInline,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        params.structureName = "";
        let res = await outNextStructure(params);
        if (res.id) {
          this.showOutResult = false;
          this.isShowFoot = false;
          this.isEdit = false;
          this.formInline = res;
          this.data = [];
          this.outResultData = [];
        } else {
          this.$modal.msgWarning("已经是最后一个结构");
        }
      } catch (err) {
        console.log(err);
      } finally {
        this.nextLoading = false;
      }
    },

    //调整结果统计
    async getStatisticsResult(data,tempFlag=0) {
      try {

        let params = {
          factoryCode: data.factoryCode,
          year: data.year,
          month: data.month,
          productionVersion: data.productionVersion,
          // tempFlag: tempFlag,
        };
        console.log("调用统计接口",tempFlag,params);
        let res = await statisticsResult(params);
        let resultList = [];
        if (this.activeName == "three") {
          resultList = this.data;
        } else {
          resultList = this.outResultData;
        }

        let list = this.insertDataAfterEachName(resultList, res.rows);
        console.log("list", list);
        if (this.activeName == "three") {
          this.data = list;
        } else {
          this.outResultData = this.enrichProductTypeCode(list);
        }
      } catch (err) {
        console.log(err);
      } finally {
      }
    },

    //调整结果插入数据
    insertDataAfterEachName(arr, statistList) {
      if (!arr.length) return [];

      const result = [];
      for (let i = 0; i < arr.length; i++) {
        const current = arr[i];
        const next = arr[i + 1];
        // 添加当前数据
        result.push(current);
        console.log(current.structureName);
        // 如果下一个元素不存在或structureName不同，说明这是当前分组的最后一项
        if (!next || next.structureName !== current.structureName) {
          console.log(i);
          // 在当前分组后插入两条数据
          for (let i = 0; i < statistList.length; i++) {
            if (statistList[i].structureName == current.structureName) {
              let embryoCount = {
                structureName: current.structureName,
                showBackground: "light-green",
              };
              let lhMachines = {
                structureName: current.structureName,
                showBackground: "light-blue",
              };
              if (this.activeName == "three") {
                embryoCount.cxMachineCode = "胎胚种类数";
                lhMachines.cxMachineCode = "硫化机台数";
              } else {
                embryoCount.materialCode = "胎胚种类数";
                lhMachines.materialCode = "硫化机台数";
              }
              for (let j = 1; j <= 31; j++) {
                const key = `day${j}`;

                if (statistList[i][key]) {
                  let dayData = JSON.parse(statistList[i][key]);
                  // embryoCount.push{
                  //   `day${j}`:dayData.EmbryoCount
                  // }
                  embryoCount[key] = dayData.embryoCount;
                  lhMachines[key] = dayData.lhMachines;
                }
              }
              result.push(embryoCount);
              result.push(lhMachines);
            }
          }
        }
      }

      return result;
    },

    //渲染统计颜色
    tableRowClassName({ row, rowIndex }) {
      if (row.showBackground) {
        return row.showBackground;
      }
      if (row.adjustFlag == 1) {
        return "warning-row";
      }
      return "";
    },

    /**
     * 调用 getPreviousStructure（outGetStayDay）：与旧版一致，传 **listAdjusts 选中行全部字段**（id、monthPlanVersion、planType 等），
     * 仅合并 this.query 补缺并从 yearMonth 解析年月；去掉 UI 临时字段；beginDay/endDay/year/month 做数值化。
     */
    async getStartDay(date) {
      if (!date || typeof date !== "object") {
        return;
      }
      const payload = { ...date };
      delete payload._isNew;
      delete payload._tmpId;

      const q = this.query || {};

      if (!payload.factoryCode && q.factoryCode) {
        payload.factoryCode = q.factoryCode;
      }
      const needYm =
        payload.year == null ||
        payload.year === "" ||
        payload.month == null ||
        payload.month === "";
      if (needYm && q.yearMonth) {
        const arr = String(q.yearMonth).split("-");
        if (arr.length >= 2) {
          payload.year = Number(arr[0]);
          payload.month = Number(arr[1]);
        }
      } else {
        if (payload.year != null && payload.year !== "") {
          payload.year = Number(payload.year);
        }
        if (payload.month != null && payload.month !== "") {
          payload.month = Number(payload.month);
        }
      }

      if (!payload.cxMachineCode && q.scheduledMachines) {
        payload.cxMachineCode = q.scheduledMachines;
      }
      if (!payload.cxMachineCode && q.cxMachineCode) {
        payload.cxMachineCode = q.cxMachineCode;
      }

      if (!payload.productionVersion && q.productionVersion) {
        payload.productionVersion = q.productionVersion;
      }

      if (
        payload.beginDay !== undefined &&
        payload.beginDay !== "" &&
        payload.beginDay != null
      ) {
        payload.beginDay = Number(payload.beginDay);
      }
      if (
        payload.endDay !== undefined &&
        payload.endDay !== "" &&
        payload.endDay != null
      ) {
        payload.endDay = Number(payload.endDay);
      }

      if (Object.keys(payload).length === 0) {
        return;
      }

      try {
        const res = await outGetStayDay(payload);
        if (res && res.adjustStartDay != null) {
          this.$set(this.formInline, "adjustStartDay", res.adjustStartDay);
        }
      } catch (err) {
        console.log(err);
      }
    },

    /**
     * 月计划调整查询弹窗中点击「选择」后携带参数进入：展示单结构调整表单并走与「单选结构调整」相同的数据加载逻辑
     */
    applyMonthPlanFinalSelectPrefill() {
      const q = this.$route.query || {};
      this.monthPlanFromFinalSelect = true;

      let storedRow = null;
      if (q.prefillStore === "1") {
        try {
          const raw = sessionStorage.getItem(
            MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY
          );
          if (raw) {
            storedRow = JSON.parse(raw);
          }
        } catch (e) {
          console.warn("结构行缓存解析失败", e);
        }
        try {
          sessionStorage.removeItem(MP_STRUCTURE_ADJUST_PREFILL_STORAGE_KEY);
        } catch (e) {
          /* ignore */
        }
      }

      /** 与周程 handleAdd 一致：formInline = 完整 listAdjusts 行；路由仅覆盖首台机台等导航字段 */
      if (storedRow && typeof storedRow === "object") {
        this.formInline = { ...storedRow };
        if (q.cxMachineCode) {
          this.formInline.cxMachineCode = q.cxMachineCode;
        }
        if (q.scheduledMachines) {
          this.formInline.scheduledMachines = q.scheduledMachines;
        }
        if (q.structureName) {
          this.formInline.structureName = q.structureName;
        }
        if (q.beginDay !== undefined && q.beginDay !== "") {
          this.formInline.beginDay = Number(q.beginDay);
        }
        if (q.endDay !== undefined && q.endDay !== "") {
          this.formInline.endDay = Number(q.endDay);
        }
        const qPv =
          q.productionVersion != null &&
          String(q.productionVersion).trim() !== ""
            ? String(q.productionVersion).trim()
            : "";
        const mergedPv =
          qPv ||
          (this.formInline.productionVersion != null
            ? String(this.formInline.productionVersion).trim()
            : "");
        if (mergedPv) {
          this.formInline.productionVersion = mergedPv;
        }

        const sched = (
          q.scheduledMachines ||
          q.cxMachineCode ||
          this.formInline.scheduledMachines ||
          this.formInline.cxMachineCode ||
          ""
        ).toString();

        this.search = {
          ...this.search,
          factoryCode:
            q.factoryCode ||
            this.formInline.factoryCode ||
            this.search.factoryCode,
          yearMonth: q.yearMonth || this.search.yearMonth,
          scheduledMachines: sched,
          productionVersion: mergedPv,
        };
        const rowAdjVer =
          this.formInline.version != null &&
          String(this.formInline.version).trim() !== ""
            ? String(this.formInline.version).trim()
            : "";
        if (rowAdjVer) {
          this.search.version = rowAdjVer;
        }
        this.query = { ...this.search };
        this.adjustType = "02";
        this.show = false;
        this.loading = true;
        this.isEdit = false;
        this.data = [];
        this.actionDate = { ...this.formInline };
        this.getStartDay(this.formInline);
        /** 与 handleAdd 一致：仅拉版本下拉，不 getOutHistoryList(true)，否则 isEdit=false 主表只读 */
        if ((this.formInline.productionVersion || "").trim()) {
          this.getOutVersionList();
        } else {
          this.page = null;
          setTimeout(() => {
            this.page = null;
            this.data = [];
            this.show = true;
            this.loading = false;
            this.isShowResult = true;
            this.activeName = "singleResult";
          }, 500);
        }
        return;
      }

      const qPv =
        q.productionVersion != null &&
        String(q.productionVersion).trim() !== ""
          ? String(q.productionVersion).trim()
          : "";
      this.search = {
        ...this.search,
        factoryCode: q.factoryCode || this.search.factoryCode,
        yearMonth: q.yearMonth || this.search.yearMonth,
        scheduledMachines: q.scheduledMachines || q.cxMachineCode || "",
        /** 与周程结构调整 Tab getVersionList 写入 query 一致，保证 getOutList 的 ...this.query 带定稿版本 */
        productionVersion: qPv,
      };
      this.query = { ...this.search };
      this.adjustType = "02";
      let year = null;
      let month = null;
      const ymStr = this.search.yearMonth;
      if (ymStr) {
        const arr = String(ymStr).split("-");
        if (arr.length >= 2) {
          year = Number(arr[0]);
          month = Number(arr[1]);
        }
      }
      /** 与 listAdjusts 列表行字段对齐，便于 getStartDay / 后续接口使用 */
      const row = {
        factoryCode: this.search.factoryCode,
        year,
        month,
        cxMachineCode: q.cxMachineCode || "",
        structureName: q.structureName || "",
        beginDay:
          q.beginDay !== undefined && q.beginDay !== ""
            ? Number(q.beginDay)
            : "",
        endDay:
          q.endDay !== undefined && q.endDay !== "" ? Number(q.endDay) : "",
        productionVersion: qPv,
        scheduledMachines: q.scheduledMachines || q.cxMachineCode || "",
      };
      this.formInline = { ...row };
      this.actionDate = row;
      this.show = false;
      this.loading = true;
      this.isEdit = false;
      this.data = [];
      this.getStartDay(row);
      if (row.productionVersion) {
        this.getOutVersionList();
      } else {
        this.page = null;
        setTimeout(() => {
          this.page = null;
          this.data = [];
          this.show = true;
          this.loading = false;
          this.isShowResult = true;
          this.activeName = "singleResult";
        }, 500);
      }
    },

  },
  mounted() {
    // console.log("mounted");
    // this.getList();
  },
  created() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    let defaultParams = {
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
      factoryCode: "116",
    };
    /** 从月计划调整查询页「结构内调整」进入时，路由仅带 yearMonth，需与来源页查询年月一致 */
    const rq = this.$route.query || {};
    if (rq.yearMonth != null && String(rq.yearMonth).trim() !== "") {
      const normalized = this.formatYearMonthForPicker(
        String(rq.yearMonth).trim()
      );
      if (normalized) {
        defaultParams.yearMonth = normalized;
      }
    }
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    if (this.pageVariant === "structureAdjust") {
      this.adjustType = "02";
      this.activeName = "second";
      this.page = {
        current: 1,
        pageSize: 20,
        total: 0,
      };
    } else {
      this.adjustType = "01";
      this.activeName = "first";
      this.page = null;
    }
    const isMpPrefill =
      this.pageVariant === "structureAdjust" &&
      this.$route.query.fromSelect === "1";
    if (isMpPrefill) {
      this.applyMonthPlanFinalSelectPrefill();
    } else {
      this.getVersionList(true);
    }
  },
};
</script>
<style lang="scss" scoped>
::v-deep .el-table__fixed,
::v-deep .el-table__fixed-right {
  background-color: #fff;
}
// ::v-deep .el-table__fixed-body-wrapper .el-table__body td,
// ::v-deep .el-table__fixed-right .el-table__body td {
//   background-color: #fff;
// }
.el-table__fixed-body-wrapper .light-green > td,
.el-table__fixed-right-body-wrapper .light-greenr > td {
  background-color: #e2efda !important;
}

.el-table__fixed-body-wrapper .light-blue > td,
.el-table__fixed-right-body-wrapper .light-blue > td {
  background-color: #9bc2e6 !important;
}

.el-table__fixed-body-wrapper .warning-row > td,
.el-table__fixed-right-body-wrapper .warning-row > td {
  background-color: #ffcccc !important;
}
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.expend-table {
  padding: 5px 10px;
  width: 100%;
  position: relative;
  z-index: 100;
}
:deep(.el-table__expand-icon) {
  display: none;
}
::v-deep .light-green {
  background-color: #e2efda!important;
}
::v-deep .light-blue {
  background-color: #9bc2e6!important;
}
::v-deep .warning-row {
  background-color: #ffcccc!important;
}

.mp-structure-inner-header {
  margin-bottom: 4px;
}
.mp-structure-inner-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

/* 行内表单中占满一行，使调整日期与按钮换到下一行 */
.mp-roll-adjust-toolbar-line {
  display: block;
  width: 100%;
  margin-top: 8px;
}

</style>
