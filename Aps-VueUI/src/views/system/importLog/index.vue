<template>
  <div class="app-container" v-loading="loading">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item :label="this.$t('common.api.importLog.columnname.functionName')" prop="functionName">
        <el-input
          v-model="queryParams.functionName"
          :placeholder="this.$t('common.api.importLog.placeholder.functionName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label-width="120px" :label="this.$t('common.api.importLog.columnname.fileName')" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          :placeholder="this.$t('common.api.importLog.placeholder.fileName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.createDate')" prop="dataArray">
        <el-date-picker
          v-model="queryParams.dataArray"
          style="width: 240px"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          :start-placeholder="this.$t('common.startDate')"
          :end-placeholder="this.$t('common.endDate')"
          :default-time="['00:00:00', '23:59:59']"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          @click="handleQuery"
          >{{$t("common.button.search")}}</el-button
        >
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery"
          >{{$t("common.button.reset")}}</el-button
        >
      </el-form-item>
    </el-form>

    <t-table
      ref="tables"
      height='calc(100vh - 260px)'
      :data="list"
      @selection-change="handleSelectionChange"
      :default-sort="defaultSort"
      @sort-change="handleSortChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="50" align="center" />
      <!-- <t-table-column label="ID" align="center" prop="id" /> -->
      <t-table-column
        :label="$t('common.api.importLog.columnname.functionCode')"
        align="center"
        prop="functionCode"
        :show-overflow-tooltip="true"
      />
      <t-table-column :label="$t('common.api.importLog.columnname.functionName')" align="center" prop="functionName" />
      <t-table-column
        :label="$t('common.api.importLog.columnname.fileName')"
        align="center"
        prop="fileName"
        width="110"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          {{ scope.row.fileName }}
          <!-- <text-button @click="handleDownload(scope.row)">{{
            scope.row.fileName
          }}</text-button> -->
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.importLog.columnname.successNum')"
        align="center"
        prop="successNum"
        width="130"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.importLog.columnname.failNum')"
        align="center"
        prop="failNum"
        width="130"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.createByName')"
        align="center"
        prop="createBy"
        width="130"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.createDate')"
        align="center"
        prop="createTime"
        width="180"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </t-table-column>

      <t-table-column
        :label="$t('common.option')"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row, scope.index)"
            >{{$t("common.api.importLog.columnname.viewErrorLog")}}</el-button
          >
        </template>
      </t-table-column>
    </t-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 操作日志详细 -->
    <el-dialog
      :title="$t('common.api.importLog.title.importLogDetail')"
      :visible.sync="open"
      width="1000px"
      append-to-body
      @close="handleClose"
    >
      <t-table
        ref="tables"
        v-loading="loadingDialog"
        :data="detailList"
        max-height="500px"
        border
        :empty-text="this.$t('common.emptyDataDescription')"
        :sum-text="this.$t('common.sum')"
      >
        <!-- <t-table-column label="ID" align="center" prop="id" minWidth="100"/> -->
        <t-table-column
          :label="$t('common.api.importLog.columnname.errorRow')"
          align="center"
          prop="errorRow"
          :show-overflow-tooltip="true"
          minWidth="80"
        />
        <t-table-column
          :label="$t('common.api.importLog.columnname.errorDetail')"
          align="center"
          prop="errorDetail"
          minWidth="500"
          :show-overflow-tooltip="true"
        >
          <template slot-scope="scope">
            <div :style="{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'normal' }">
              {{ scope.row.errorDetail }}
            </div>
        </template>
        </t-table-column>
        <t-table-column
          :label="$t('common.createDate')"
          align="center"
          prop="createTime"
          minWidth="200"
        />
        <!-- <t-table-column label="错误类型" align="center" prop="errorType" /> -->
      </t-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">{{$t("common.button.close")}}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { list, errorDietailList, cleanOperlog } from "@/api/system/importLog";
// import { list } from "@/api/system/logininfor";
import { tansParams, parseTime } from "@/utils/ruoyi";
export default {
  name: "importLog",
  dicts: ["sys_oper_type", "sys_common_status"],
  data() {
    return {
      // 遮罩层
      loading: true,
      loadingDialog:true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 表格数据
      list: [
        // {
        //   id: 254424,
        //   functionCode: "功能code",
        //   functionName: "功能名称",
        //   fileName: "导入文件名称",
        //   fileUrl: "xxx.html",
        //   successNum: 100,
        //   failNum: 0,
        //   createBy: "xxx",
        //   createTime: "2024-02-23 14:31:41",
        // },
        // {
        //   id: 254430,
        //   functionCode: "功能code2",
        //   functionName: "功能名称",
        //   fileName: "导入文件名称",
        //   successNum: 100,
        //   failNum: 0,
        //   createBy: "xxx",
        //   createTime: "2024-02-23 14:31:41",
        // },
      ],
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 默认排序
      defaultSort: { prop: "operTime", order: "descending" },
      // 表单参数
      form: {},
      // 查询参数
      queryParams: {
        functionName: undefined,
        fileName: undefined,
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        status: undefined,
        dataArray: [],
      },
      //错误日志详情列表
      detailList: [],
    };
  },
  created() {
    //设置默认查询日期，当前日期到一周前的日期；
    let endDate = new Date();
    let startDate = new Date(endDate.getTime() - 7 * 24 * 3600 * 1000);
    endDate.setHours(23, 59, 59, 999);
    startDate.setHours(0, 0, 0, 0);
    // console.log(startDate,endDate,parseTime(startDate,"{y}-{m}-{d} {h}:{i}:{s}"));
    this.queryParams.dataArray = [
      parseTime(startDate, "{y}-{m}-{d} {h}:{i}:{s}"),
      parseTime(endDate, "{y}-{m}-{d} {h}:{i}:{s}"),
    ];
    this.getList();
  },
  methods: {
    /** 查询登录日志 */
    async getList() {
      try {
        if (
          !this.queryParams.dataArray ||
          this.queryParams.dataArray.length < 2
        ) {
          this.$modal.msgError(this.$t("common.api.importLog.error.dataArray"));
          return;
        }
        // console.log(this.queryParams.dataArray);
        this.loading = true;
        const data = await list(this.queryParams);
        this.list = data.rows;
        this.total = data.total;
        console.log(data);
        // list(this.addDateRange(this.queryParams)).then((response) => {
        //   this.list = response.rows;
        //   this.total = response.total;
        // });
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    /**错误日志详情列表 */
    async getDetail(importLogId) {
      try {
        this.loadingDialog = true;
        const params = {
          importLogId: importLogId,
        };
        const response = await errorDietailList(params);
        this.detailList = response.rows;
        this.detailTotal = response.total;
        // errorDietailList(params).then((response) => {
        //   this.detailList = response.rows;
        //   // this.detailList.push(...response.rows);
        //   this.detailTotal = response.total;
        // });
      } catch (error) {
        console.log(error);
      } finally {
        this.loadingDialog = false;
      }
    },
    /**关闭清空弹窗表格数据 */
    handleClose() {
      this.detailList = [];
    },
    /**导入日志下载 */
    async handleDownload(row) {
      // try {
      //   let params = {
      //     name: row.fileName,
      //     url: row.fileUrl,
      //   };
      //   let downloadDom = document.createElement("a");
      //   downloadDom.href =
      //     process.env.VUE_APP_BASE_API +
      //     "/system/importLog/download" +
      //     "?" +
      //     tansParams(params);
      //   document.body.appendChild(downloadDom);
      //   downloadDom.click();
      //   document.body.removeChild(downloadDom);
      // } catch (error) {
      //   console.log(error);
      // }
    },
    // 操作日志类型字典翻译
    typeFormat(row, column) {
      return this.selectDictLabel(
        this.dict.type.sys_oper_type,
        row.businessType
      );
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.queryParams.pageNum = 1;
      this.$refs.tables.sort(this.defaultSort.prop, this.defaultSort.order);
      this.getList();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.operId);
      this.multiple = !selection.length;
    },
    /** 排序触发事件 */
    handleSortChange(column, prop, order) {
      this.queryParams.orderByColumn = column.prop;
      this.queryParams.isAsc = column.order;
      this.getList();
    },
    /** 详细按钮操作 */
    handleView(row) {
      //获取数据
      this.getDetail(row.id);
      this.open = true;
      // this.form = row;
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(
        "monitor/operlog/export/vue",
        {
          ...this.queryParams,
        },
        `operlog_${new Date().getTime()}.xlsx`
      );
    },
  },
};
</script>

