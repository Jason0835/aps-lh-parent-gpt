<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
      key="cxFixedMachineMainTable"
      ref="tableRef"
      :calcHeight="showOutResult ? false : true"
      v-loading="loading"
      element-loading-text="正在获取数据，请稍候!"
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
    >
      <template slot="header">
        <el-tabs v-model="activeName" @tab-click="handleClick" type="card">
          <el-tab-pane label="结构内" name="first" :disabled="loading">
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
          </el-tab-pane>
          <el-tab-pane label="结构调整" name="second" :disabled="loading">
            <el-button @click="handleAdd" :disabled="selection.length != 1">{{
              $t("单选结构调整")
            }}</el-button>
            <!-- <el-button @click="handleShowSpecial">{{
              $t("特殊材料生产情况")
            }}</el-button> -->
            <el-button
              @click="handleAddSpecial"
              v-hasPermi="['monthplan:mpStructureAllocation:save']"
              >{{ $t("新增结构") }}</el-button
            >
          </el-tab-pane>
          <el-tab-pane
            label="单结构调整"
            disabled
            name="singleResult"
            v-if="isShowResult"
          >
            <el-form
              :inline="true"
              :model="formInline"
              class="demo-form-inline"
            >
              <el-form-item label="机台">
                <el-input
                  v-model="formInline.cxMachineCode"
                  disabled
                  placeholder="机台"
                ></el-input>
              </el-form-item>
              <el-form-item label="产品结构">
                <el-input
                  v-model="formInline.structureName"
                  disabled
                  placeholder="产品结构"
                ></el-input>
              </el-form-item>
              <el-form-item label="开始日期">
                <el-input
                  disabled
                  v-model="formInline.beginDay"
                  style="width: 50px"
                  placeholder="开始日期"
                ></el-input>
              </el-form-item>
              <el-form-item label="结束日期">
                <el-input
                  disabled
                  style="width: 50px"
                  v-model="formInline.endDay"
                  placeholder="结束日期"
                ></el-input>
              </el-form-item>
              <el-form-item label="调整开始日期">
                <el-select
                  v-model="formInline.adjustStartDay"
                  style="width: 100px"
                  disabled
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
              <el-form-item v-if="showOutResult">
                <el-button
                  type="primary"
                  @click="nextStructure"
                  :loading="nextLoading"
                  >下一个</el-button
                >
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane
            label="调整结果"
            name="three"
             :disabled="loading"
            v-if="this.hasPermission('monthplan:mpAdjustResult:list')"
          >
          </el-tab-pane>
        </el-tabs>
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
        </div>
      </template>
    </page-table>
    <div v-if="showOutResult">
      <el-table
        :data="outResultData"
        border
        stripe
        style="width: 100%"
        max-height="250"
      >
        <el-table-column
          v-for="item in outResultColumns"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
          :fixed="item.fixed ? true : false"
        >
        </el-table-column>
      </el-table>
      <div
        v-if="showConfirmResult"
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
    </div>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />

    <special ref="specialRef"></special>
    <addModal ref="addModalRef" @success="addSuccessFun" />
  </basic-container>
</template>
<script>
//lib
import { mapState, mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

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
} from "@/api/monthplan/adjustStructure";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import result from "./components/result.vue";
import special from "./components/special.vue";
import addModal from "./components/addModal.vue";
export default {
  name: "MoldingClosingStageProgress",
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
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      //结构外调整结果列表
      outResultData: [],
      outResultVersion: "",
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

      dayList: 31,
    };
  },
  computed: {
    ...mapGetters("globalList", ["structureList"]),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
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
            width: 320,
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
                      row.confirmAdjustQty = value.replace(/\./g, "");
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
            // render: ({ row }) => {
            //   return (
            //     <div
            //       style={{ background: row.actualAdjustQty ? "yellow" : "" }}
            //     >
            //       {row.actualAdjustQty}
            //     </div>
            //   );
            // },
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
            width: 320,
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
                  {!this.isTabChange && (
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
                  {this.isTabChange && (
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
            width: 320,
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
                        row.confirmAdjustQty = value.replace(/\./g, "");
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
        {
          prop: "scheduledMachines",
          label: this.$t("成型机台"),
        },
        {
          prop: "structureName",
          label: this.$t("产品结构"),
          type: "select",
          dictData: this.structureList,
          filterable: true,
        },
        {
          prop: this.activeName == "second" ? "productionVersion" : "version",
          label: this.$t("版本号"),
          type: "select",
          clearable: this.activeName == "first" ? false : true,
          filterable: true,
          dictData: this.versionList,
        },
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
        },
      ];
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
        });
      }
      return list;
    },
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
          width: 320,
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
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
      ];
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
        });
      }
      return list;
    },
  },
  methods: {
    refreshSearch(){
      this.search={
        factoryCode:this.search.factoryCode,
        yearMonth:this.search.yearMonth,
        productionVersion:this.search.productionVersion,
        version:this.search.version,

      }
      this.query={
        factoryCode:this.search.factoryCode,
        yearMonth:this.search.yearMonth,
        productionVersion:this.search.productionVersion,
        version:this.search.version,
      }
      this.getList()
    },
    //查询是否有权限
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    //修改锁定上机日期
    handleLockScheduleChange(row, val) {
      saveAdjustResult(row)
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          // this.getList();
        })
        .catch((err) => {
          console.log(err);
        });
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
    async editAdjust(row, type) {
      // if (!type) {
      //   if (!this.isNoPositiveInteger(row.confirmAdjustQty)) {
      //     return this.$modal.msgWarning("不能有小数点");
      //   }
      // } else {
      //   if (!this.isPositiveInteger(row.adjustPriority)) {
      //     return this.$modal.msgWarning("请输入正整数");
      //   }
      // }

      try {
        let res = await saveAdjust(row);
        this.$modal.msgSuccess(res.msg);
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
      try {
        let res = await editOutHistory(row);
        this.$modal.msgSuccess(res.msg);
        // this.getList();
      } catch (err) {}
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

      this.getVersionList();
    },
    handleMaterialCodeChange(val) {
      console.log(val)
      this.search = {
        ...this.search,
        materialCode: val,
      };
      this.query = {
        ...this.search,
        materialCode: val,
      };

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
    async getVersionList(isGet = false) {
      this.loading=true
      let res;
      try {
        if (this.activeName == "first") {
          res = await versionAdjust(this.formatParams());
        }
        if (this.activeName == "second") {
          res = await versionStructure(this.formatParams());
        }
        if (this.activeName == "three") {
          if (!this.isTabChange) return;
          res = await resultVersion(this.formatParams());
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
          if (this.activeName != "second") {
            this.$set(this.search, "version", list[0].value);
            this.$set(this.query, "version", list[0].value);
          } else {
            this.$set(this.search, "productionVersion", list[0].value);
            this.$set(this.query, "productionVersion", list[0].value);
          }
        } else {
          if (this.activeName != "second") {
            this.$set(this.search, "version", "");
            this.$set(this.query, "version", "");
          } else {
            this.$set(this.search, "productionVersion", "");
            this.$set(this.query, "productionVersion", "");
          }
        }
        // if (isGet) {
        // this.$nextTick(() => {
        //   this.getList();
        // });
        // }
      } catch (err) {
        console.log(err);
      } finally {
        if (isGet) {
          this.getList();
        }else{
          this.loading=false
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
      this.show = false;
      this.showConfirmResult = false;
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
        this.getVersionList();
        this.loading = false;
      } catch (err) {
        this.getLoading = false;
        this.loading = false;
      }
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
        this.getSubList(row);
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

    //tab切换
    handleClick(tab, event) {

      // this.loading = true;
      this.showConfirmResult = false;
      this.show = false;
      this.isShowResult = false;
      this.isShowFoot = false;
      this.showOutResult = false;
      console.log('this.activeName',this.activeName)
      if (this.activeName == "three") {
        this.isTabChange = true;
        this.page = {
          current: 1,
          pageSize: 20,
          total: 0,
        };
        this.getVersionList(true);

        return;
      }
      if (this.activeName == "second") {
        this.page = {
          current: 1,
          pageSize: 20,
          total: 0,
        };
        this.adjustType = "02";
      } else {
        this.adjustType = "01";
        this.page = null;
      }
      // this.getList();
      this.getVersionList(true);
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

    //结构自动调整
    async handShowResult() {
      this.show = false;
      this.loading = true;
      this.autoLoading = true;
      try {
        let params = {
          ...this.query,
          ...this.sort,
          adjustType: this.adjustType,
          version: this.data[0]?.version,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        let res = await autoAdjust(params);
        console.log(res);
        this.data = res;
        // this.data=res.rows
        this.show = true;
        this.loading = false;
        this.autoLoading = false;
        this.isTabChange = false;
        this.isShowFoot = true;
        this.activeName = "three";
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
      console.log(this.data);
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
      this.autoLoading = true;
      try {
        let params = {
          ...this.query,
          ...this.sort,
          ...this.formInline,
          adjustType: this.adjustType,
          version: this.data[0]?.version,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        console.log("params", params);
        params.startDay = params.beginDay;
        params.scheduledMachines = params.cxMachineCode;
        let res = await autoAdjust(params);
        this.outResultData = res;
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
        this.autoLoading = false;
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

        params.adjustType = this.adjustType;
        this.isEdit = true;
        let res = await getAdjustDetailList(params);
        this.data = res.rows;
        this.getOutResultList(res.rows[0].productionVersion,res.rows[0].version);
        this.isShowFoot = true;
        this.showOutResult = true;
        this.formInline.adjustStartDay = this.formInline.beginDay;
        this.formInline.adjustEndDay = this.formInline.endDay;

        this.getOutVersionList();
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
      }
    },

    //结构外初始化结构列表
    async getOutResultList(productionVersion,version) {
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
        this.outResultData = res.rows;
        console.log("初始化");
        console.log(res);
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
    handleOutDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeOutHistory({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.resizeOutHistoryList();
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
      if (this.activeName == "second") {
        this.$set(this.page, "current", 1);
      }
      if (this.activeName == "singleResult") {
        // this.resizeOutHistoryList();
        return;
      }

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
    handleExport() {
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
    },

    formatParams() {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (this.activeName == "second" || this.activeName == "three") {
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
        } else {
          return;
        }
        this.data = data.rows;
        if (this.activeName == "second" || this.activeName == "three") {
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
  },
  mounted() {
    // console.log("mounted");
    // this.getList();
  },
  created() {
    // 获取当前ui.data.colume.year和月份
    const now = new Date();
    const year = now.getFullYear(); // 2024
    const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
    let defaultParams = {
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    // this.getList();
    this.getVersionList(true);
  },
  activated() {
    // console.log('activated')
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
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
</style>
