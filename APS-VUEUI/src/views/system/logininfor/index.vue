<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item :label="this.$t('common.api.logininfo.columnname.ipaddr')" prop="ipaddr">
        <el-input
          v-model="queryParams.ipaddr"
          :placeholder="this.$t('common.api.logininfo.placeholder.ipaddr')"
          clearable
          style="width: 240px;"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="this.$t('common.api.logininfo.columnname.userName')" prop="userName">
        <el-input
          v-model="queryParams.userName"
          :placeholder="this.$t('common.api.logininfo.placeholder.userName')"
          clearable
          style="width: 240px;"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="this.$t('common.api.logininfo.columnname.loginStatus')"
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
      <el-form-item :label="this.$t('common.api.logininfo.columnname.loginTime')">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          :start-placeholder="this.$t('common.startDate')"
          :end-placeholder="this.$t('common.endDate')"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">{{$t("common.button.search")}}</el-button>
        <el-button icon="el-icon-refresh" size="mini" :loading="loading" @click="resetQuery">{{$t("common.button.reset")}}</el-button>
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
          v-hasPermi="['monitor:logininfor:remove']"
        >{{$t("common.button.delete")}}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          @click="handleClean"
          v-hasPermi="['monitor:logininfor:remove']"
        >{{$t("common.button.clear")}}</el-button>
      </el-col>
      <!-- <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-unlock"
          size="mini"
          :disabled="single"
          @click="handleUnlock"
          v-hasPermi="['monitor:logininfor:unlock']"
        >解锁</el-button>
      </el-col> -->
      <!-- <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['monitor:logininfor:export']"
        >导出</el-button>
      </el-col> -->
      <right-toolbar tableRef="loginInfoTable" :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <t-table ref="loginInfoTable" height='calc(100vh - 300px)' v-loading="loading" :data="list" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange" border :empty-text="this.$t('common.emptyDataDescription')" :sum-text="this.$t('common.sum')">
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column :label="$t('common.api.logininfo.columnname.infoId')" align="center" prop="infoId" />
      <t-table-column :label="$t('common.api.logininfo.columnname.userName')" align="center" prop="userName" :show-overflow-tooltip="true" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <t-table-column :label="$t('common.api.logininfo.columnname.addr')" align="center" prop="ipaddr" width="130" :show-overflow-tooltip="true" />
      <t-table-column :label="$t('common.api.logininfo.columnname.loginStatus')" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_common_status" :value="scope.row.status"/>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.api.logininfo.columnname.detail')" align="center" prop="msg" :show-overflow-tooltip="true" />
      <t-table-column :label="$t('common.api.logininfo.columnname.accessTime')" align="center" prop="accessTime" sortable="custom" :sort-orders="['descending', 'ascending']" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.accessTime) }}</span>
        </template>
      </t-table-column>
    </t-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { tansParams } from "@/utils/ruoyi";
import { list, delLogininfor, cleanLogininfor, unlockLogininfor } from "@/api/system/logininfor";

export default {
 name: "Logininfor",
  dicts: ['sys_common_status'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 选择用户名
      selectName: "",
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 表格数据
      list: [],
      // 日期范围
      dateRange: [],
      // 默认排序
      defaultSort: {prop: 'accessTime', order: 'descending'},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipaddr: undefined,
        userName: undefined,
        status: undefined
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询登录日志列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await list(this.addDateRange(this.queryParams, this.dateRange));
      this.list = response.rows;
        this.total = response.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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
      this.$refs.tables.sort(this.defaultSort.prop, this.defaultSort.order)
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.infoId)
      this.single = selection.length!=1
      this.multiple = !selection.length
      this.selectName = selection.map(item => item.userName);
    },
    /** 排序触发事件 */
    handleSortChange(column, prop, order) {
      this.queryParams.orderByColumn = column.prop;
      this.queryParams.isAsc = column.order;
      this.getList();
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const infoIds = row.infoId || this.ids;
      this.$modal
        // .confirm('是否确认删除访问编号为"' + infoIds + '"的数据项？')
        .confirm(this.$t("common.api.logininfo.confirm.detete",{infoIds}))
        .then(function() {
        return delLogininfor(infoIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
      }).catch(() => {});
    },
    /** 清空按钮操作 */
    handleClean() {
      this.$modal.confirm(this.$t('common.api.logininfo.confirm.clearAll')).then(function() {
        return cleanLogininfor();
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$t("common.msg.success.clear"));
      }).catch(() => {});
    },
    /** 解锁按钮操作 */
    handleUnlock() {
      const username = this.selectName;
      this.$modal.confirm('是否确认解锁用户"' + username + '"数据项?').then(function() {
        return unlockLogininfor(username);
      }).then(() => {
        this.$modal.msgSuccess("用户" + username + "解锁成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
            try {
        let params = {
          ...this.queryParams
        };
        let downloadDom = document.createElement("a");
        downloadDom.href =
          process.env.VUE_APP_BASE_API +
          "monitor/logininfor/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.download('monitor/logininfor/export/vue', {
      //   ...this.queryParams
      // }, `logininfor_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

