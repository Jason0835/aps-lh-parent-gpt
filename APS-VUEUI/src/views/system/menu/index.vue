<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item
        :label="$t('common.api.sysmenu.columnname.menuName')"
        prop="menuName"
      >
        <el-input
          v-model="queryParams.menuName"
          :placeholder="$t('common.api.sysmenu.placeholder.menuName')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('common.api.sysmenu.columnname.menuStatus')"
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
      <el-form-item
        :label="$t('common.api.sysmenu.columnname.visible')"
        prop="visible"
      >
        <el-select
          v-model="queryParams.visible"
          :placeholder="$t('common.api.sysmenu.columnname.visible')"
          clearable
        >
          <el-option
            v-for="dict in dict.type.sys_show_hide"
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
          v-hasPermi="['system:menu:add']"
          >{{ $t("common.button.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-sort"
          size="mini"
          @click="toggleExpandAll"
          >{{ $t("common.button.expandCollapse") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="menuTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      ref="menuTable"
      height="calc(100vh - 220px)"
      v-if="refreshTable"
      v-loading="loading"
      :data="menuList"
      row-key="menuId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column
        prop="menuName"
        :label="$t('common.api.sysmenu.columnname.menuName')"
        :show-overflow-tooltip="true"
        minWidth="200"
      ></t-table-column>
      <t-table-column
        prop="icon"
        :label="$t('common.api.sysmenu.columnname.icon')"
        align="center"
        width="100"
      >
        <template slot-scope="scope">
          <svg-icon :icon-class="scope.row.icon" />
        </template>
      </t-table-column>
      <t-table-column
        prop="orderNum"
        :label="$t('common.api.sysmenu.columnname.orderNum')"
        width="60"
      ></t-table-column>
      <t-table-column
        prop="btPerms"
        :label="$t('common.api.sysmenu.columnname.btPerms')"
        :show-overflow-tooltip="true"
      ></t-table-column>
      <t-table-column
        prop="component"
        :label="$t('common.api.sysmenu.columnname.componentPath')"
        :show-overflow-tooltip="true"
      ></t-table-column>
      <t-table-column prop="status" :label="$t('common.status')" width="80">
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_normal_disable"
            :value="scope.row.status"
          />
        </template>
      </t-table-column>
      <t-table-column
        prop="visible"
        :label="$t('common.api.sysmenu.columnname.visible')"
        width="80"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_show_hide"
            :value="scope.row.visible"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.createTime')"
        align="center"
        prop="createTime"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.option')"
        align="center"
        class-name="small-padding fixed-width"
        minWidth="100"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:menu:edit']"
            >{{ $t("common.button.modify") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:menu:add']"
            >{{ $t("common.button.add") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:menu:remove']"
            >{{ $t("common.button.delete") }}</el-button
          >
        </template>
      </t-table-column>
    </el-table>

    <!-- 添加或修改菜单对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="680px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form
        class="form-item-height"
        ref="form"
        :model="form"
        :rules="rules"
        label-width="100px"
        v-loading="dialogLoading"
      >
        <el-row>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.parentId')"
              prop="parentId"
            >
              <treeselect
                v-model="form.parentId"
                :options="menuOptions"
                :normalizer="normalizer"
                :show-count="true"
                :placeholder="$t('common.api.sysmenu.placeholder.parentId')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.menuType')"
              prop="menuType"
            >
              <el-radio-group v-model="form.menuType">
                <el-radio label="M">{{
                  $t("common.api.sysmenu.columnname.mTypeM")
                }}</el-radio>
                <el-radio label="C">{{
                  $t("common.api.sysmenu.columnname.mTypeC")
                }}</el-radio>
                <el-radio label="F">{{
                  $t("common.api.sysmenu.columnname.mTypeF")
                }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.menuType != 'F'">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.menuIcon')"
              prop="icon"
            >
              <el-popover
                placement="bottom-start"
                width="460"
                trigger="click"
                @show="$refs['iconSelect'].reset()"
              >
                <IconSelect
                  ref="iconSelect"
                  @selected="selected"
                  :active-icon="form.icon"
                />
                <el-input
                  slot="reference"
                  v-model="form.icon"
                  :placeholder="$t('common.api.sysmenu.placeholder.icon')"
                  readonly
                >
                  <svg-icon
                    v-if="form.icon"
                    slot="prefix"
                    :icon-class="form.icon"
                    style="width: 25px"
                  />
                  <i
                    v-else
                    slot="prefix"
                    class="el-icon-search el-input__icon"
                  />
                </el-input>
              </el-popover>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.menuName')"
              prop="menuName"
            >
              <el-input
                v-model="form.menuName"
                :placeholder="$t('common.api.sysmenu.placeholder.menuName')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.visualOrder')"
              prop="orderNum"
            >
              <el-input-number
                v-model="form.orderNum"
                controls-position="right"
                :min="0"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'F'">
            <el-form-item prop="isFrame">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.isFrame')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.isFrame") }}
              </span>
              <el-radio-group v-model="form.isFrame">
                <el-radio label="0">{{ $t("common.yes") }}</el-radio>
                <el-radio label="1">{{ $t("common.no") }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'F'">
            <el-form-item prop="path">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.path')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.path") }}
              </span>
              <el-input
                v-model="form.path"
                :placeholder="$t('common.api.sysmenu.placeholder.path')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType == 'C'">
            <el-form-item prop="component">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.componentPath')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.componentPath") }}
              </span>
              <el-input
                v-model="form.component"
                :placeholder="
                  $t('common.api.sysmenu.placeholder.componentPath')
                "
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'M'">
            <el-form-item prop="btPerms">
              <el-input
                v-model="form.btPerms"
                :placeholder="$t('common.api.sysmenu.placeholder.btPerms')"
                maxlength="100"
              />
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.btPerms')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.btPermsChar") }}
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType == 'C'">
            <el-form-item prop="query">
              <el-input
                v-model="form.query"
                :placeholder="$t('common.api.sysmenu.placeholder.query')"
                maxlength="255"
              />
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.query')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.query") }}
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType == 'C'">
            <el-form-item prop="isCache">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.isCache')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.isCache") }}
              </span>
              <el-radio-group v-model="form.isCache">
                <el-radio label="0">{{
                  $t("common.api.sysmenu.columnname.cache")
                }}</el-radio>
                <el-radio label="1">{{
                  $t("common.api.sysmenu.columnname.noChahe")
                }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != 'F'">
            <el-form-item prop="visible">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.visible')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.visible") }}
              </span>
              <el-radio-group v-model="form.visible">
                <el-radio
                  v-for="dict in dict.type.sys_show_hide"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="status">
              <span slot="label">
                <el-tooltip
                  :content="$t('common.api.sysmenu.tips.menuStatus')"
                  placement="top"
                >
                  <i class="el-icon-question"></i>
                </el-tooltip>
                {{ $t("common.api.sysmenu.columnname.menuStatus") }}
              </span>
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
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.sysmenu.columnname.langPackage')"
              prop="langJson"
            >
              <el-input
                v-model="form.langJson"
                :placeholder="$t('common.api.sysmenu.placeholder.langPackage')"
              />
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
        <el-button :loading="dialogLoading" @click="cancel">{{
          $t("common.button.cancel")
        }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listMenu,
  getMenu,
  delMenu,
  addMenu,
  updateMenu,
} from "@/api/system/menu";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import IconSelect from "@/components/IconSelect";

export default {
  name: "/system/menu",
  dicts: ["sys_show_hide", "sys_normal_disable"],
  components: { Treeselect, IconSelect },
  data() {
    return {
      // 遮罩层
      loading: true,
      dialogLoading: false,
      // 显示搜索条件
      showSearch: true,
      // 菜单表格树数据
      menuList: [],
      // 菜单树选项
      menuOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部折叠
      isExpandAll: false,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        menuName: undefined,
        visible: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        menuName: [
          {
            required: true,
            message: this.$t("common.api.sysmenu.error.name.isnull"),
            trigger: "blur",
          },
        ],
        orderNum: [
          {
            required: true,
            message: this.$t("common.api.sysmenu.error.orderNum.isnull"),
            trigger: "blur",
          },
        ],
        path: [
          {
            required: true,
            message: this.$t("common.api.sysmenu.error.path.isnull"),
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
    // 选择图标
    selected(name) {
      this.form.icon = name;
    },
    /** 查询菜单列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listMenu(this.queryParams);
        this.menuList = this.handleTree(response, "menuId");
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    /** 转换菜单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.menuId,
        label: node.menuName,
        children: node.children,
      };
    },
    /** 查询菜单下拉树结构 */
    getTreeselect() {
      listMenu().then((response) => {
        this.menuOptions = [];
        const menu = {
          menuId: 0,
          menuName: this.$t("common.api.sysmenu.columnname.mainCategories"),
          children: [],
        };
        menu.children = this.handleTree(response, "menuId");
        this.menuOptions.push(menu);
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
        menuId: undefined,
        parentId: 0,
        menuName: undefined,
        icon: undefined,
        menuType: "M",
        orderNum: undefined,
        isFrame: "1",
        isCache: "0",
        visible: "0",
        status: "0",
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.menuId) {
        this.form.parentId = row.menuId;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = this.$t("common.api.sysmenu.title.addMenu");
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      getMenu(row.menuId).then((response) => {
        this.form = response;
        this.open = true;
        this.title = this.$t("common.api.sysmenu.title.modifyMenu");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            if (this.form.menuId != undefined) {
              const response = await updateMenu(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addMenu(this.form);
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
      this.$modal
        // .confirm('是否确认删除名称为"' + row.menuName + '"的数据项？')
        .confirm(
          this.$t("common.api.sysmenu.confirm.detete", {
            menuName: row.menuName,
          })
        )
        .then(function () {
          return delMenu(row.menuId);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
        })
        .catch(() => {});
    },
  },
};
</script>
