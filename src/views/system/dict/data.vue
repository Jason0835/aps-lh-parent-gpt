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
        prop="dictType"
      >
        <el-select v-model="queryParams.dictType">
          <el-option
            v-for="item in typeOptions"
            :key="item.dictId"
            :label="item.dictName"
            :value="item.dictType"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="$t('common.api.dictData.columnname.label')"
        prop="dictLabel"
      >
        <el-input
          v-model="queryParams.dictLabel"
          :placeholder="$t('common.api.dictData.placeholder.dataLabel')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('common.api.dictData.placeholder.dataStatus')"
          clearable
        >
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          type="warning"
          plain
          icon="el-icon-close"
          size="mini"
          @click="handleClose"
          >{{ $t("common.button.close") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="dictDataTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="dictDataTable"
      height="calc(100vh - 300px)"
      v-loading="loading"
      :data="dataList"
      @selection-change="handleSelectionChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column
        :label="$t('common.api.dictData.columnname.code')"
        align="center"
        prop="dictCode"
      />
      <t-table-column
        :label="$t('common.api.dictData.columnname.label')"
        align="center"
        prop="dictLabel"
      >
        <template slot-scope="scope">
          <span
            v-if="
              (scope.row.listClass == '' || scope.row.listClass == 'default') &&
              (scope.row.cssClass == '' || scope.row.cssClass == null)
            "
            >{{ scope.row.dictLabel }}</span
          >
          <el-tag
            v-else
            :type="scope.row.listClass == 'primary' ? '' : scope.row.listClass"
            :class="scope.row.cssClass"
            >{{ scope.row.dictLabel }}</el-tag
          >
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.dictData.columnname.value')"
        align="center"
        prop="dictValue"
      />
      <t-table-column
        :label="$t('common.api.dictData.columnname.sort')"
        align="center"
        prop="dictSort"
      />
      <t-table-column :label="$t('common.status')" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_normal_disable"
            :value="scope.row.status"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.dictData.columnname.lang')"
        align="locale"
        prop="locale"
      />
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
        <el-form-item :label="$t('common.api.dictData.columnname.dictType')">
          <el-input v-model="form.dictType" :disabled="true" />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictData.columnname.dataLabel')"
          prop="dictLabel"
        >
          <el-input
            v-model="form.dictLabel"
            :placeholder="$t('common.api.dictData.placeholder.dataLabel')"
            maxlength="40"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictData.columnname.dataValue')"
          prop="dictValue"
        >
          <el-input
            v-model="form.dictValue"
            :placeholder="$t('common.api.dictData.placeholder.dataValue')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictData.columnname.cssClass')"
          prop="cssClass"
        >
          <el-input
            v-model="form.cssClass"
            :placeholder="$t('common.api.dictData.placeholder.cssClass')"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictData.columnname.displaySort')"
          prop="dictSort"
        >
          <el-input-number
            v-model="form.dictSort"
            controls-position="right"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item
          :label="$t('common.api.dictData.columnname.listClass')"
          prop="listClass"
        >
          <el-select v-model="form.listClass">
            <el-option
              v-for="item in listClassOptions"
              :key="item.value"
              :label="item.label + '(' + item.value + ')'"
              :value="item.value"
            ></el-option>
          </el-select>
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
        <el-form-item
          :label="$t('common.api.dictData.columnname.lang')"
          prop="locale"
        >
          <el-input
            v-model="form.locale"
            :placeholder="$t('common.api.dictData.placeholder.lang')"
            maxlength="5"
          />
        </el-form-item>
        <el-form-item :label="$t('common.remark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="$t('common.api.dictData.placeholder.remark')"
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
  listData,
  getData,
  delData,
  addData,
  updateData,
} from "@/api/system/dict/data";
import {
  optionselect as getDictOptionselect,
  getType,
} from "@/api/system/dict/type";

export default {
  name: "Data",
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
      dataList: [],
      // 默认字典类型
      defaultDictType: "",
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 数据标签回显样式
      listClassOptions: [
        {
          value: "default",
          label: this.$t("common.api.dictData.listClass.default"),
        },
        {
          value: "primary",
          label: this.$t("common.api.dictData.listClass.primary"),
        },
        {
          value: "success",
          label: this.$t("common.api.dictData.listClass.success"),
        },
        {
          value: "info",
          label: this.$t("common.api.dictData.listClass.info"),
        },
        {
          value: "warning",
          label: this.$t("common.api.dictData.listClass.warning"),
        },
        {
          value: "danger",
          label: this.$t("common.api.dictData.listClass.danger"),
        },
      ],
      // 类型数据字典
      typeOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        dictType: undefined,
        dictLabel: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        dictLabel: [
          {
            required: true,
            message: this.$t("common.api.dictData.error.dictLabel.isnull"),
            trigger: "blur",
          },
        ],
        dictValue: [
          {
            required: true,
            message: this.$t("common.api.dictData.error.dictValue.isnull"),
            trigger: "blur",
          },
        ],
        dictSort: [
          {
            required: true,
            message: this.$t("common.api.dictData.error.dictSort.isnull"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  created() {
    const dictId = this.$route.params && this.$route.params.dictId;
    this.getType(dictId);
    this.getTypeList();
  },
  methods: {
    /** 查询字典类型详细 */
    getType(dictId) {
      getType(dictId).then((response) => {
        this.queryParams.dictType = response.dict.dictType;
        this.defaultDictType = response.dict.dictType;
        this.getList();
      });
    },
    /** 查询字典类型列表 */
    getTypeList() {
      getDictOptionselect().then((response) => {
        this.typeOptions = response;
      });
    },
    /** 查询字典数据列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listData(this.queryParams);
        this.dataList = response.rows;
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
        dictCode: undefined,
        dictLabel: undefined,
        dictValue: undefined,
        cssClass: undefined,
        listClass: "default",
        dictSort: 0,
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
    /** 返回按钮操作 */
    handleClose() {
      const obj = { path: "/system/dict" };
      this.$tab.closeOpenPage(obj);
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.dictType = this.defaultDictType;
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.$t("common.api.dictData.title.addDictData");
      this.form.dictType = this.queryParams.dictType;
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.dictCode);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const dictCode = row.dictCode || this.ids;
      getData(dictCode).then((response) => {
        this.form = response;
        this.open = true;
        this.title = this.$t("common.api.dictData.title.modifyDictData");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            if (this.form.dictCode != undefined) {
              const response = await updateData(this.form);
              this.$store.dispatch(
                "dict/removeDict",
                this.queryParams.dictType
              );
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addData(this.form);
              this.$store.dispatch(
                "dict/removeDict",
                this.queryParams.dictType
              );
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
      let dictCodes = row.dictCode || this.ids;
      if (!Array.isArray(dictCodes)) {
        dictCodes = [dictCodes];
      }
      this.$modal
        // .confirm('是否确认删除字典编码为"' + dictCodes + '"的数据项？')
        .confirm(this.$t("common.api.dictData.confirm.detete", { dictCodes }))
        .then(function () {
          return delData(dictCodes);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
          this.$store.dispatch("dict/removeDict", this.queryParams.dictType);
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
          "/system/dict/data/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.download('system/dict/data/export/vue', {
      //   ...this.queryParams
      // }, `data_${new Date().getTime()}.xlsx`)
    },
  },
};
</script>
