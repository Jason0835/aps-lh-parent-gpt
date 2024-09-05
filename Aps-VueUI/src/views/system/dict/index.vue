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
        :label="$t('common.api.dictType.columnname.name')"
        prop="dictName"
      >
        <el-input
          v-model="queryParams.dictName"
          :placeholder="$t('common.api.dictType.placeholder.dictName')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.dictType.columnname.dictType')"
        prop="dictType"
      >
        <el-input
          v-model="queryParams.dictType"
          :placeholder="$t('common.api.dictType.placeholder.dictType')"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('common.api.dictType.placeholder.dictStatus')"
          clearable
          style="width: 240px"
        >
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
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
          v-hasPermi="['system:dict:add']"
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
          v-hasPermi="['system:dict:edit']"
          >{{ $t("common.button.modify") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:dict:remove']"
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
          v-hasPermi="['system:dict:export']"
        >导出</el-button>
      </el-col> -->
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="handleRefreshCache"
          v-hasPermi="['system:dict:remove']"
          >{{ $t("common.button.refreshCache") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="dictTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="dictTable"
      height="calc(100vh - 300px)"
      v-loading="loading"
      :data="typeList"
      @selection-change="handleSelectionChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column
        :label="$t('common.api.dictType.columnname.dictId')"
        align="center"
        prop="dictId"
      />
      <t-table-column
        :label="$t('common.api.dictType.columnname.name')"
        align="center"
        prop="dictName"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.dictType.columnname.dictType')"
        align="center"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <router-link
            :to="'/system/dict-data/index/' + scope.row.dictId"
            class="link-type"
          >
            <span>{{ scope.row.dictType }}</span>
          </router-link>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.status')" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_normal_disable"
            :value="scope.row.status"
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
            v-hasPermi="['system:dict:edit']"
            >{{ $t("common.button.modify") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:dict:remove']"
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
          :label="$t('common.api.dictType.columnname.name')"
          prop="dictName"
        >
          <el-input
            v-model="form.dictName"
            :placeholder="$t('common.api.dictType.placeholder.dictName')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictType.columnname.dictType')"
          prop="dictType"
        >
          <el-input
            v-model="form.dictType"
            :placeholder="$t('common.api.dictType.placeholder.dictType')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item :label="$t('common.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
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
            :placeholder="$t('common.api.dictType.placeholder.remark')"
            maxlength="150"
          ></el-input>
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
  listType,
  getType,
  delType,
  addType,
  updateType,
  refreshCache,
} from "@/api/system/dict/type";

export default {
  name: "/system/dict",
  dicts: ["sys_normal_disable"],
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
      // 字典表格数据
      typeList: [],
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
        dictName: undefined,
        dictType: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        dictName: [
          {
            required: true,
            message: this.$t("common.api.dictType.error.name.isnull"),
            trigger: "blur",
          },
        ],
        dictType: [
          {
            required: true,
            message: this.$t("common.api.dictType.error.dictType.isnull"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询字典类型列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listType(
          this.addDateRange(this.queryParams, this.dateRange)
        );
        this.typeList = response.rows;
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
        dictId: undefined,
        dictName: undefined,
        dictType: undefined,
        status: "0",
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
      this.title = this.$t("common.api.dictType.title.addDictType");
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.dictId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const dictId = row.dictId || this.ids;
      getType(dictId).then((response) => {
        this.form = response.dict;
        this.open = true;
        this.title = this.$t("common.api.dictType.title.modifyDictType");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            if (this.form.dictId != undefined) {
              const response = await updateType(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addType(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.add"));
            }
            this.open = false;
            this.getList();
          } catch (error) {
            console.error(error);
          } finally {
            this.dialogLoading = false;
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      let dictIds = row.dictId || this.ids;
      if (!Array.isArray(dictIds)) {
        dictIds = [dictIds];
      }
      this.$modal
        // .confirm('是否确认删除字典编号为"' + dictIds + '"的数据项？')
        .confirm(this.$t("common.api.dictType.confirm.detete", { dictIds }))
        .then(function () {
          return delType(dictIds);
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
          "/system/dict/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.download('system/dict/export/vue', {
      //   ...this.queryParams
      // }, `type_${new Date().getTime()}.xlsx`)
    },
    /** 刷新缓存按钮操作 */
    handleRefreshCache() {
      refreshCache().then(() => {
        this.$modal.msgSuccess(this.$t("common.msg.success.refresh"));
        this.$store.dispatch("dict/cleanDict");
      });
    },
  },
};
</script>
