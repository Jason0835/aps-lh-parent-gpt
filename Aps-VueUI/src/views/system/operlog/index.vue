<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item
        :label="$t('common.api.operlog.columnname.operIp')"
        prop="operIp"
      >
        <el-input
          v-model="queryParams.operIp"
          :placeholder="$t('common.api.operlog.placeholder.operIp')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.operlog.columnname.sysModules')"
        prop="title"
      >
        <el-input
          v-model="queryParams.title"
          :placeholder="$t('common.api.operlog.placeholder.sysModules')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.operlog.columnname.operName')"
        prop="operName"
      >
        <el-input
          v-model="queryParams.operName"
          :placeholder="$t('common.api.operlog.placeholder.operName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.type')" prop="businessType">
        <el-select
          v-model="queryParams.businessType"
          :placeholder="$t('common.api.operlog.columnname.businessType')"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.sys_oper_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('common.api.operlog.columnname.operStatus')"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.sys_common_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.api.operlog.columnname.operTime')">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('common.startDate')"
          :end-placeholder="$t('common.endDate')"
          :default-time="['00:00:00', '23:59:59']"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          :loading="loading"
          @click="handleQuery"
          >{{ $t("common.button.search") }}</el-button
        >
        <el-button
          icon="el-icon-refresh"
          size="mini"
          :loading="loading"
          @click="resetQuery"
          >{{ $t("common.button.reset") }}</el-button
        >
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:operlog:remove']"
          >{{ $t("common.button.delete") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          @click="handleClean"
          v-hasPermi="['monitor:operlog:remove']"
          >{{ $t("common.button.clear") }}</el-button
        >
      </el-col>
      <!-- <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['monitor:operlog:export']"
        >导出</el-button>
      </el-col> -->
      <right-toolbar
        tableRef="operLogTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="operLogTable"
      height="calc(100vh - 320px)"
      v-loading="loading"
      :data="list"
      @selection-change="handleSelectionChange"
      :default-sort="defaultSort"
      @sort-change="handleSortChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="50" align="center" />
      <t-table-column
        :label="$t('common.api.operlog.columnname.operId')"
        align="center"
        prop="operId"
      />
      <t-table-column
        :label="$t('common.api.operlog.columnname.sysModules')"
        align="center"
        prop="title"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.operlog.columnname.businessType')"
        align="center"
        prop="businessType"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_oper_type"
            :value="scope.row.businessType"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.operlog.columnname.requestMethod')"
        align="center"
        prop="requestMethod"
      />
      <t-table-column
        :label="$t('common.api.operlog.columnname.operName')"
        align="center"
        prop="operName"
        width="110"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.operlog.columnname.operIp')"
        align="center"
        prop="operIp"
        width="130"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.operlog.columnname.operStatus')"
        align="center"
        prop="status"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_common_status"
            :value="scope.row.status"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.operlog.columnname.operTime')"
        align="center"
        prop="operTime"
        width="180"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.operTime) }}</span>
        </template>
      </t-table-column>
      <!-- <t-table-column
        label="消耗时间"
        align="center"
        prop="costTime"
        width="110"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.costTime }}毫秒</span>
        </template>
      </t-table-column> -->
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
            v-hasPermi="['monitor:operlog:query']"
            >{{ $t("common.button.detail") }}</el-button
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
      :title="$t('common.api.operlog.title.operLogDetail')"
      :visible.sync="open"
      width="700px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form ref="form" :model="form" label-width="100px" size="mini">
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.operModules')"
              >{{ form.title }} / {{ typeFormat(form) }}</el-form-item
            >
            <el-form-item :label="$t('common.api.operlog.formLabel.operIp')"
              >{{ form.operName }} / {{ form.operIp }}</el-form-item
            >
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('common.api.operlog.formLabel.operUrl')">{{
              form.operUrl
            }}</el-form-item>
            <el-form-item
              :label="$t('common.api.operlog.formLabel.requestMethod')"
              >{{ form.requestMethod }}</el-form-item
            >
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('common.api.operlog.formLabel.method')">{{
              form.method
            }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.operParam')"
              >{{ form.operParam }}</el-form-item
            >
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.jsonResult')"
              >{{ form.jsonResult }}</el-form-item
            >
          </el-col>
          <el-col :span="6">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.operStatus')"
            >
              <div v-if="form.status === 0">
                {{ $t("common.api.operlog.formLabel.normal") }}
              </div>
              <div v-else-if="form.status === 1">
                {{ $t("common.api.operlog.formLabel.fail") }}
              </div>
            </el-form-item>
          </el-col>
          <!-- <el-col :span="8">
            <el-form-item :label="$t('common.api.operlog.formLabel.costTime')">{{ form.costTime }}毫秒</el-form-item>
          </el-col> -->
          <el-col :span="10">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.operTime')"
              >{{ parseTime(form.operTime) }}</el-form-item
            >
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.operlog.formLabel.errorMsg')"
              v-if="form.status === 1"
              >{{ form.errorMsg }}</el-form-item
            >
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">{{
          $t("common.button.close")
        }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { tansParams } from "@/utils/ruoyi";
import { list, delOperlog, cleanOperlog } from "@/api/system/operlog";

export default {
  name: "operlog",
  dicts: ["sys_oper_type", "sys_common_status"],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 表格数据
      list: [],
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
        pageNum: 1,
        pageSize: 10,
        operIp: undefined,
        title: undefined,
        operName: undefined,
        businessType: undefined,
        status: undefined,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询登录日志 */
    async getList() {
      try {
        this.loading = true;
        const response = await list(
          this.addDateRange(this.queryParams, this.dateRange)
        );
        this.list = response.rows;
        this.total = response.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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
      this.open = true;
      this.form = row;
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      let operIds = row.operId || this.ids;
      if (!Array.isArray(operIds)) {
        operIds = [operIds];
      }
      this.$modal
        // .confirm('是否确认删除日志编号为"' + operIds + '"的数据项？')
        .confirm(this.$t("common.api.operlog.confirm.detete", { operIds }))
        .then(function () {
          return delOperlog(operIds);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
        })
        .catch(() => {});
    },
    /** 清空按钮操作 */
    handleClean() {
      this.$modal
        .confirm(this.$t("common.api.operlog.confirm.clearAll"))
        .then(function () {
          return cleanOperlog();
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.clear"));
        })
        .catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      try {
        let params = {
          ...this.queryParams,
        };
        let downloadDom = document.createElement("a");
        downloadDom.href =
          process.env.VUE_APP_BASE_API +
          "/monitor/operlog/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.download('monitor/operlog/export/vue', {
      //   ...this.queryParams
      // }, `operlog_${new Date().getTime()}.xlsx`)
    },
  },
};
</script>

