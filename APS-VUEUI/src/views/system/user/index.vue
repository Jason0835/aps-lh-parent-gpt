<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!--部门数据-->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="deptName"
            :placeholder="$t('common.api.dept.placeholder.deptName')"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container" :height="availableHeight">
          <el-tree
            :style="{ overflowX: 'auto', height: availableHeight + 100 + 'px' }"
            :data="deptOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            ref="tree"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>
      <!--用户数据-->
      <el-col :span="20" :xs="24">
        <el-form
          :model="queryParams"
          ref="queryForm"
          size="small"
          :inline="true"
          v-show="showSearch"
          label-width="68px"
        >
          <el-form-item
            :label="$t('common.api.user.columnname.username')"
            prop="userName"
          >
            <el-input
              maxlength="10"
              v-model="queryParams.userName"
              :placeholder="$t('common.api.user.placeholder.userName')"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item
            :label="$t('common.api.user.columnname.telphone')"
            prop="phonenumber"
          >
            <el-input
              maxlength="11"
              v-model="queryParams.phonenumber"
              :placeholder="$t('common.api.user.placeholder.phoneNumber')"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item :label="$t('common.status')" prop="status">
            <el-select
              v-model="queryParams.status"
              :placeholder="$t('common.api.user.columnname.userStatus')"
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
              v-hasPermi="['system:user:add']"
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
              v-hasPermi="['system:user:edit']"
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
              v-hasPermi="['system:user:remove']"
              >{{ $t("common.button.delete") }}</el-button
            >
          </el-col>
          <!-- <el-col :span="1.5">
            <el-button
              type="info"
              plain
              icon="el-icon-upload2"
              size="mini"
              @click="handleImport"
              v-hasPermi="['system:user:import']"
            >导入</el-button>
          </el-col>-->
          <!-- <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="el-icon-download"
              size="mini"
              @click="handleExport"
              v-hasPermi="['system:user:export']"
            >导出</el-button>
          </el-col> -->
          <right-toolbar
            tableRef="userTable"
            :showSearch.sync="showSearch"
            @queryTable="getList"
          ></right-toolbar>
        </el-row>

        <t-table
          ref="userTable"
          v-loading="loading"
          :data="userList"
          @selection-change="handleSelectionChange"
          :height="availableHeight"
          border
          :empty-text="this.$t('common.emptyDataDescription')"
          :sum-text="this.$t('common.sum')"
        >
          <t-table-column type="selection" width="50" align="center" />
          <t-table-column
            :label="$t('common.api.user.columnname.usercode')"
            align="center"
            key="userId"
            prop="userId"
            v-if="columns[0].visible"
          />
          <t-table-column
            :label="$t('common.api.user.columnname.username')"
            align="center"
            key="userName"
            prop="userName"
            v-if="columns[1].visible"
            :show-overflow-tooltip="true"
          />
          <t-table-column
            :label="$t('common.api.user.columnname.workNo')"
            align="center"
            key="workNo"
            prop="workNo"
            v-if="columns[2].visible"
          />
          <t-table-column
            :label="$t('common.api.user.columnname.nickname')"
            align="center"
            key="nickName"
            prop="nickName"
            v-if="columns[3].visible"
            :show-overflow-tooltip="true"
          />
          <t-table-column
            :label="$t('common.api.user.columnname.dept.dept')"
            align="center"
            key="deptName"
            prop="dept.deptName"
            v-if="columns[4].visible"
            :show-overflow-tooltip="true"
          />
          <t-table-column
            :label="$t('common.api.user.columnname.telphone')"
            align="center"
            key="phonenumber"
            prop="phonenumber"
            v-if="columns[5].visible"
            width="120"
          />
          <t-table-column
            :label="$t('common.status')"
            align="center"
            key="status"
            v-if="columns[6].visible"
          >
            <template slot-scope="scope">
              <el-switch
                v-model="scope.row.status"
                active-value="0"
                inactive-value="1"
                @change="handleStatusChange(scope.row)"
              ></el-switch>
            </template>
          </t-table-column>
          <t-table-column
            :label="$t('common.createTime')"
            align="center"
            prop="createTime"
            v-if="columns[7].visible"
            width="160"
          >
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </t-table-column>
          <t-table-column
            :label="$t('common.option')"
            align="center"
            width="160"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope" v-if="scope.row.userId !== 1">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['system:user:edit']"
                >{{ $t("common.button.modify") }}</el-button
              >
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['system:user:remove']"
                >{{ $t("common.button.delete") }}</el-button
              >
              <el-dropdown
                size="mini"
                @command="(command) => handleCommand(command, scope.row)"
                v-hasPermi="['system:user:resetPwd', 'system:user:edit']"
              >
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-d-arrow-right"
                  >{{ $t("common.button.more") }}</el-button
                >
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item
                    command="handleResetPwd"
                    icon="el-icon-key"
                    v-hasPermi="['system:user:resetPwd']"
                    >{{
                      $t("common.api.user.columnname.resetPwd")
                    }}</el-dropdown-item
                  >
                  <el-dropdown-item
                    command="handleAuthRole"
                    icon="el-icon-circle-check"
                    v-hasPermi="['system:user:edit']"
                    >{{
                      $t("common.api.user.columnname.assignRole")
                    }}</el-dropdown-item
                  >
                </el-dropdown-menu>
              </el-dropdown>
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
      </el-col>
    </el-row>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="600px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form
        v-loading="dialogLoading"
        class="form-item-height"
        ref="form"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.user.columnname.nickname')"
              prop="nickName"
            >
              <el-input
                v-model="form.nickName"
                :placeholder="$t('common.api.user.placeholder.nickname')"
                maxlength="10"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.user.columnname.dept.belongDept')"
              prop="deptId"
            >
              <treeselect
                v-model="form.deptId"
                :options="deptOptions"
                :show-count="true"
                :placeholder="$t('common.api.user.placeholder.belongDept')"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.user.columnname.telphone')"
              prop="phonenumber"
            >
              <el-input
                v-model="form.phonenumber"
                :placeholder="$t('common.api.user.placeholder.phoneNumber')"
                maxlength="11"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.user.columnname.email')"
              prop="email"
            >
              <el-input
                v-model="form.email"
                :placeholder="$t('common.api.user.placeholder.email')"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              v-if="form.userId == undefined"
              :label="$t('common.api.user.columnname.username')"
              prop="userName"
            >
              <el-input
                v-model="form.userName"
                :placeholder="$t('common.api.user.placeholder.userName')"
                maxlength="10"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              v-if="form.userId == undefined"
              :label="$t('common.api.user.columnname.password')"
              prop="password"
            >
              <el-input
                v-model="form.password"
                :placeholder="$t('common.api.user.placeholder.password')"
                type="password"
                maxlength="20"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('common.api.user.columnname.sex')">
              <el-select
                v-model="form.sex"
                :placeholder="$t('common.api.user.placeholder.sex')"
              >
                <el-option
                  v-for="dict in dict.type.sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('common.status')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.user.columnname.workNo')"
              prop="workNo"
            >
              <el-input
                v-model="form.workNo"
                :placeholder="$t('common.api.user.placeholder.workNo')"
                maxlength="10"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('common.api.user.columnname.post')">
              <el-select
                v-model="form.postIds"
                multiple
                :placeholder="$t('common.api.user.placeholder.post')"
              >
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('common.api.user.columnname.role')">
              <el-select
                v-model="form.roleIds"
                multiple
                :placeholder="$t('common.api.user.placeholder.role')"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == 1"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('common.remark')">
              <el-input
                maxlength="150"
                v-model="form.remark"
                type="textarea"
                :placeholder="$t('common.api.user.placeholder.remark')"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :loading="dialogLoading"
          @click="submitForm"
          >{{ $t("common.button.confirm") }}</el-button
        >
        <el-button @click="cancel">{{ $t("common.button.cancel") }}</el-button>
      </div>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog
      :title="upload.title"
      :visible.sync="upload.open"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        :auto-upload="false"
        v-loading="upload.submitLoading"
        :on-change="handleChange"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          {{ $t("common.upload.dragFileText")
          }}<em>{{ $t("common.upload.clickUpload") }}</em>
        </div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox
              v-model="upload.updateSupport"
            />是否更新已经存在的用户数据
          </div>
          <span>{{ $t("common.upload.onlyXlsXlsx") }}</span>
          <el-link
            type="primary"
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            @click="importTemplate"
            >{{ $t("common.upload.downloadTemplate") }}</el-link
          >
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :disabled="upload.submitLoading || upload.fileList.length == 0"
          @click="submitFileForm"
          >{{ $t("common.button.confirm") }}</el-button
        >
        <el-button @click="upload.open = false">{{
          $t("common.button.cancel")
        }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
  import {debounce} from "@/utils";
  import {addUser, changeUserStatus, delUser, deptTreeSelect, getUser, listUser, resetUserPwd, updateUser,} from "@/api/system/user";
  import {tansParams} from "@/utils/ruoyi";
  import {getToken} from "@/utils/auth";
  import Treeselect from "@riophae/vue-treeselect";
  import "@riophae/vue-treeselect/dist/vue-treeselect.css";

  export default {
  name: "/system/user",
  dicts: ["sys_normal_disable", "sys_user_sex"],
  components: { Treeselect },
  data() {
    return {
      //表格高度设置
      availableHeight: 500,
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
      // 用户表格数据
      userList: null,
      // 弹出层标题
      title: "",
      // 部门树选项
      deptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 部门名称
      deptName: undefined,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label",
      },
      // 用户导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/user/importData",
        submitLoading: false,
        //文件列表
        fileList: [],
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined,
      },
      // 列信息
      columns: [
        {
          key: 0,
          label: this.$t(`common.api.user.columnname.usercode`),
          visible: true,
        },
        {
          key: 1,
          label: this.$t(`common.api.user.columnname.username`),
          visible: true,
        },
        {
          key: 2,
          label: this.$t(`common.api.user.columnname.workNo`),
          visible: true,
        },
        {
          key: 3,
          label: this.$t(`common.api.user.columnname.nickname`),
          visible: true,
        },
        {
          key: 4,
          label: this.$t(`common.api.user.columnname.dept.dept`),
          visible: true,
        },
        {
          key: 5,
          label: this.$t(`common.api.user.columnname.telphone`),
          visible: true,
        },
        { key: 6, label: this.$t(`common.status`), visible: true },
        { key: 7, label: this.$t(`common.createTime`), visible: true },
      ],
      // 表单校验
      rules: {
        deptId: [
          {
            required: true,
            message: this.$t("common.api.user.error.deptId.isnull"),
            trigger: "blur",
          },
        ],
        userName: [
          {
            required: true,
            message: this.$t("common.api.user.error.userName.isnull"),
            trigger: "blur",
          },
          {
            min: 2,
            max: 20,
            message: this.$t("common.api.user.error.userName.lengthLimit"),
            trigger: "blur",
          },
        ],
        nickName: [
          {
            required: true,
            message: this.$t("common.api.user.error.nickname.isnull"),
            trigger: "blur",
          },
        ],
        password: [
          {
            required: true,
            message: this.$t("common.api.user.error.password.isnull"),
            trigger: "blur",
          },
          {
            min: 5,
            max: 20,
            message: this.$t("common.api.user.error.password.lengthLimit"),
            trigger: "blur",
          },
        ],
        email: [
          {
            type: "email",
            message: this.$t("common.rule.email"),
            trigger: ["blur", "change"],
          },
        ],
        phonenumber: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: this.$t("common.rule.phone"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
  },
  created() {
    this.getList();
    this.getDeptTree();
    this.getConfigKey("sys.user.initPassword").then((response) => {
      this.initPassword = response.msg;
    });

    this._resizeHandler = debounce(() => {
      this.$nextTick(() => {
        this.setTableHeight();
      });
    }, 100);
    window.addEventListener("resize", this._resizeHandler, false);
  },
  mounted() {
    this.$nextTick(() => {
      this.setTableHeight();
    });
  },
  beforeDestroy() {
    // if (this.calcHeight) {
      window.removeEventListener("resize", this._resizeHandler, false);
    // }
  },
  methods: {
    setTableHeight() {
      let formHeight = this.$refs.queryForm.$el.clientHeight;
      console.log(formHeight);
      let tempHeight = window.innerHeight - formHeight - 200;
      this.availableHeight = parseInt(tempHeight);
    },
    /** 查询用户列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listUser(
          this.addDateRange(this.queryParams, this.dateRange)
        );
        this.userList = response.rows;
        this.total = response.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then((response) => {
        this.deptOptions = response;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.handleQuery();
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal
        // .confirm('确认要"' + text + '""' + row.userName + '"用户吗？')
        .confirm(
          row.status === "0"
            ? this.$t("common.api.user.confirm.enableUser", {
                userName: row.userName,
              })
            : this.$t("common.api.user.confirm.disableUser", {
                userName: row.userName,
              })
        )
        .then(function () {
          return changeUserStatus(row.userId, row.status);
        })
        .then(() => {
          this.$modal.msgSuccess(
            row.status === "0"
              ? this.$t("common.msg.success.enable")
              : this.$t("common.msg.success.disable")
          );
        })
        .catch(function () {
          row.status = row.status === "0" ? "1" : "0";
        });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: "0",
        remark: undefined,
        postIds: [],
        roleIds: [],
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
      this.queryParams.deptId = undefined;
      this.$refs.tree.setCurrentKey(null);
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.userId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleResetPwd":
          this.handleResetPwd(row);
          break;
        case "handleAuthRole":
          this.handleAuthRole(row);
          break;
        default:
          break;
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      getUser().then((response) => {
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.open = true;
        this.title = this.$t("common.api.user.title.addUser");
        this.form.password = this.initPassword;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const userId = row.userId || this.ids;
      getUser(userId).then((response) => {
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.form = response.user;
        this.$set(
          this.form,
          "postIds",
          response.posts.filter((item) => item.flag).map((item) => item.postId)
        );
        this.$set(
          this.form,
          "roleIds",
          response.roles.filter((item) => item.flag).map((item) => item.roleId)
        );
        this.open = true;
        this.title = this.$t("common.api.user.title.modifyUser");
        this.form.password = "";
      });
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt(
        this.$t("common.api.user.confirm.resetPwd", { userName: row.userName }),
        this.$t("common.prompt"),
        {
          confirmButtonText: this.$t("common.button.confirm"),
          cancelButtonText: this.$t("common.button.cancel"),
          closeOnClickModal: false,
          inputPattern: /^.{6,20}$/,
          inputErrorMessage: this.$t(
            "common.api.user.error.password.lengthLimit"
          ),
        }
      )
        .then(({ value }) => {
          resetUserPwd(row.userId, value).then((response) => {
            this.$modal.msgSuccess(
              this.$t("common.api.user.msg.resetPwdSuccess") + value
            );
          });
        })
        .catch(() => {});
    },
    /** 分配角色操作 */
    handleAuthRole: function (row) {
      const userId = row.userId;
      this.$router.push("/system/user-auth/role/" + userId);
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            //如果没有选中性别默认设置为未知
            if (!this.form.sex || this.form.sex == "") this.form.sex = "2";
            if (this.form.userId != undefined) {
              const response = await updateUser(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addUser(this.form);
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
      let userIds = row.userId || this.ids;
      if (!Array.isArray(userIds)) {
        userIds = [userIds];
      }
      this.$modal
        // .confirm('是否确认删除用户编号为"' + userIds + '"的数据项？')
        .confirm(this.$t("common.api.user.confirm.deleteUser", { userIds }))
        .then(function () {
          return delUser(userIds);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
        })
        .catch(() => {});
    },
    postExcelFile(params, url) {
      //params是post请求需要的参数，url是请求url地址
      var form = document.createElement("form");
      form.style.display = "none";
      form.action = url;
      form.method = "post";
      document.body.appendChild(form);

      for (var key in params) {
        if (params[key]) {
          var input = document.createElement("input");
          input.type = "hidden";
          input.name = key;
          input.value = params[key];
          form.appendChild(input);
        }
      }

      form.submit();
      form.remove();
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
          "/system/user/export" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.postExcelFile(
      //   this.queryParams,
      //   process.env.VUE_APP_BASE_API + "system/user/export/vue"
      // );
      // this.download(
      //   "system/user/export/vue",
      //   {
      //     ...this.queryParams
      //   },
      //   `user_${new Date().getTime()}.xlsx`
      // );
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = this.$t("用户导入");
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      try {
        let params = {
          ...this.query,
        };
        let downloadDom = document.createElement("a");
        downloadDom.href =
          process.env.VUE_APP_BASE_API +
          "system/user/importTemplate" +
          "?" +
          tansParams(params);
        document.body.appendChild(downloadDom);
        downloadDom.click();
        document.body.removeChild(downloadDom);
      } catch (error) {
        console.log(error);
      }
      // this.downloadGet(
      //   "system/user/importTemplate",
      //   {},
      //   `user_template_${new Date().getTime()}.xlsx`
      // );
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.upload.submitLoading = false;
      this.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
          response.msg +
          "</div>",
        this.$t("common.upload.importResult"),
        { dangerouslyUseHTMLString: true }
      );
      this.getList();
    },
    /**文件上传失败 */
    handleFileError() {
      this.upload.submitLoading = false;
    },
    // 提交上传文件
    submitFileForm() {
      try {
        this.upload.submitLoading = true;
        this.$refs.upload.submit();
      } catch (error) {
        this.upload.submitLoading = false;
      }
    },
    handleChange(file, fileList) {
      this.upload.fileList = fileList;
      // console.log(this.upload.fileList); // 这里可以获取到当前的文件列表
    },
  },
};
</script>
