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
        :label="$t('common.api.config.columnname.name')"
        prop="configName"
      >
        <el-input
          v-model="queryParams.configName"
          :placeholder="$t('common.api.config.placeholder.configName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.config.columnname.keyName')"
        prop="configKey"
      >
        <el-input
          v-model="queryParams.configKey"
          :placeholder="$t('common.api.config.placeholder.keyName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.config.columnname.configType')"
        prop="configType"
      >
        <el-select
          v-model="queryParams.configType"
          :placeholder="$t('common.api.config.columnname.configType')"
          clearable
        >
          <el-option
            v-for="dict in dict.type.sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.createTime')">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('common.startDate')"
          :end-placeholder="$t('common.endDate')"
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
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:config:add']"
          >{{ $t("common.button.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:config:edit']"
          >{{ $t("common.button.modify") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple || deleteBtnStatus"
          @click="handleDelete"
          v-hasPermi="['system:config:remove']"
          >{{ $t("common.button.delete") }}</el-button
        >
      </el-col>
      <!-- <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:config:export']"
        >导出</el-button>
      </el-col> -->
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="handleRefreshCache"
          v-hasPermi="['system:config:remove']"
          >{{ $t("common.button.refreshCache") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="configTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="configTable"
      height="calc(100vh - 300px)"
      v-loading="loading"
      :data="configList"
      @selection-change="handleSelectionChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column
        :label="$t('common.api.config.columnname.orderNo')"
        align="center"
        prop="configId"
      />
      <t-table-column
        :label="$t('common.api.config.columnname.name')"
        align="center"
        prop="configName"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.config.columnname.keyName')"
        align="center"
        prop="configKey"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.config.columnname.value')"
        align="center"
        prop="configValue"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.config.columnname.configType')"
        align="center"
        prop="configType"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_yes_no"
            :value="scope.row.configType"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.remark')"
        align="center"
        prop="remark"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.createTime')"
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
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:config:edit']"
            >{{ $t("common.button.modify") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            :disabled="scope.row.configType == 'Y'"
            v-hasPermi="['system:config:remove']"
            >{{ $t("common.button.delete") }}</el-button
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

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form
        class="form-item-height"
        v-loading="dialogLoading"
        ref="form"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item
          :label="$t('common.api.config.columnname.name')"
          prop="configName"
        >
          <el-input
            v-model="form.configName"
            :placeholder="$t('common.api.config.placeholder.configName')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.config.columnname.keyName')"
          prop="configKey"
        >
          <el-input
            v-model="form.configKey"
            :placeholder="$t('common.api.config.placeholder.keyName')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.config.columnname.value')"
          prop="configValue"
        >
          <el-input
            v-model="form.configValue"
            :placeholder="$t('common.api.config.placeholder.configValue')"
            maxlength="300"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.config.columnname.configType')"
          prop="configType"
        >
          <el-radio-group v-model="form.configType">
            <el-radio
              v-for="dict in dict.type.sys_yes_no"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('common.remark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="$t('common.api.config.placeholder.remark')"
            maxlength="150"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :loading="dialogLoading"
          @click="submitForm"
          >{{ $t("common.button.confirm") }}</el-button
        >
        <el-button :loading="dialogLoading" @click="cancel">{{
          $t("common.button.cancel")
        }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { tansParams } from "@/utils/ruoyi";
import {
  listConfig,
  getConfig,
  delConfig,
  addConfig,
  updateConfig,
  refreshCache,
} from "@/api/system/config";

export default {
  name: "/system/config",
  dicts: ["sys_yes_no"],
  data() {
    return {
      // 遮罩层
      loading: true,
      dialogLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 参数表格数据
      configList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        configKey: undefined,
        configType: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        configName: [
          {
            required: true,
            message: this.$t("common.api.config.error.name.isnull"),
            trigger: "blur",
          },
        ],
        configKey: [
          {
            required: true,
            message: this.$t("common.api.config.error.keyName.isnull"),
            trigger: "blur",
          },
        ],
        configValue: [
          {
            required: true,
            message: this.$t("common.api.config.error.value.isnull"),
            trigger: "blur",
          },
        ],
      },
      //列表选中项
      selection: [],
    };
  },
  computed: {
    deleteBtnStatus: function () {
      if (this.selection.length >= 1) {
        for (const key in this.selection) {
          if (Object.hasOwnProperty.call(this.selection, key)) {
            const element = this.selection[key];
            if (element.configType == "Y") return true;
          }
        }
        return false;
      } else {
        return true;
      }
    },
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询参数列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listConfig(
          this.addDateRange(this.queryParams, this.dateRange)
        );
        this.configList = response.rows;
        this.total = response.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        configId: undefined,
        configName: undefined,
        configKey: undefined,
        configValue: undefined,
        configType: "Y",
        remark: undefined,
      };
      this.resetForm("form");
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
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.$t("common.api.config.title.addConfig");
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.selection = selection;
      this.ids = selection.map((item) => item.configId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const configId = row.configId || this.ids;
      getConfig(configId).then((response) => {
        this.form = response;
        this.open = true;
        this.title = this.$t("common.api.config.title.modifyConfig");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            if (this.form.configId != undefined) {
              const response = await updateConfig(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addConfig(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.add"));
            }
            this.open = false;
            this.getList();
          } catch (error) {
            console.error(error);
          } finally {
            this.dialogLoading = false;
          }
          // if (this.form.configId != undefined) {
          //   updateConfig(this.form).then(response => {
          //     this.$modal.msgSuccess("修改成功");
          //     this.open = false;
          //     this.getList();
          //   });
          // } else {
          //   addConfig(this.form).then(response => {
          //     this.$modal.msgSuccess("新增成功");
          //     this.open = false;
          //     this.getList();
          //   });
          // }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      let configIds = row.configId || this.ids;
      if (!row.configId) {
        configIds = this.ids.join(",");
      }
      this.$modal
        .confirm(this.$t("common.api.config.confirm.detete", { configIds }))
        // .confirm('是否确认删除参数编号为"' + configIds + '"的数据项？')
        .then(function () {
          return delConfig(configIds);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
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
          "/system/config/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.download(
      //   "system/config/export/vue",
      //   {
      //     ...this.queryParams
      //   },
      //   `config_${new Date().getTime()}.xlsx`
      // );
    },
    /** 刷新缓存按钮操作 */
    handleRefreshCache() {
      refreshCache().then(() => {
        this.$modal.msgSuccess(this.$t("common.msg.success.refresh"));
      });
    },
  },
};
</script>
