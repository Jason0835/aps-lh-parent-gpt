<!--
 * @Description: 待办 页面
 * @Author: qy
 * @Date: 2024/2/29
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item :label="$t('common.messageTask.msgTitle')" prop="msgTitle">
        <el-input
          v-model="queryParams.msgTitle"
          :placeholder="$t('common.rule.input')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.messageTask.msgSource')" prop="msgSource">
        <el-select v-model="queryParams.msgSource" :placeholder="$t('common.messageTask.msgSource')" clearable>
          <el-option
            v-for="dict in dict.type.msg_source"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.messageTask.msgStatus')" prop="msgStatus">
        <el-select v-model="queryParams.msgStatus" :placeholder="$t('common.messageTask.msgStatus')" clearable>
          <el-option
            v-for="dict in dict.type.msg_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">{{$t('common.button.search')}}</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">{{$t('common.button.reset')}}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
<!--      <el-col :span="1.5">-->
<!--        <el-button-->
<!--          type="success"-->
<!--          plain-->
<!--          icon="el-icon-edit"-->
<!--          size="mini"-->
<!--          :disabled="single"-->
<!--          @click="handleUpdate"-->
<!--          v-hasPermi="['system:notice:edit']"-->
<!--        >办理</el-button>-->
<!--      </el-col>-->
      <right-toolbar tableRef="MyTaskTableRef" :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <t-table ref="MyTaskTableRef" :height="availableHeight" v-loading="loading" :data="messageList" @selection-change="handleSelectionChange" border>
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column :label="$t('bd.serialNo')" align="center" type="index" width="100" />
      <t-table-column :label="$t('common.messageTask.msgType')" align="center" prop="msgType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.msg_type" :value="scope.row.msgType"/>
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.messageTask.msgTitle')"
        align="center"
        prop="msgTitle"
        :show-overflow-tooltip="true"
      />
      <t-table-column :label="$t('common.messageTask.msgContent')" align="center" prop="msgContent" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <app-link v-if="scope.row.billUrl" :to="scope.row.billUrl">{{ scope.row.msgContent }}</app-link>
          <span v-else>{{ scope.row.msgContent }}</span>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.messageTask.msgSource')" align="center" prop="msgStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.msg_source" :value="scope.row.msgSource"/>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.messageTask.msgStatus')" align="center" prop="msgStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.msg_status" :value="scope.row.msgStatus"/>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.messageTask.sendBy')" align="center" prop="sendBy" />
      <t-table-column :label="$t('common.messageTask.sendTime')" align="center" prop="sendTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </t-table-column>
      <t-table-column :label="$t('common.messageTask.receivedBy')" align="center" prop="receivedBy" />
      <t-table-column :label="$t('common.messageTask.doTime')" align="center" prop="doTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
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

    <!-- 添加或修改待办对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="待办标题" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="请输入待办标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="待办类型" prop="noticeType">
              <el-select v-model="form.noticeType" placeholder="请选择待办类型">
                <el-option
                  v-for="dict in dict.type.sys_notice_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_notice_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
              <editor v-model="form.noticeContent" :min-height="192"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { debounce } from "@/utils";
import { getNotice, delNotice, addNotice, updateNotice } from "@/api/system/notice";
import {messageListTaskMessage} from "@/api/system/message";

export default {
  name: "Task",
  dicts: ['msg_channel', 'msg_source', 'msg_status', 'msg_type'],
  props: {
    // 待办类型 my-pending: 我的待办 my-processed:我的已办理
    type: {
      type: String
    }
  },
  data() {
    return {
      availableHeight:500,
      // 遮罩层
      loading: false,
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
      // 表格数据
      messageList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        receivedBy: 'admin',
        msgTitle: undefined,
        msgStatus: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        noticeTitle: [
          { required: true, message: "待办标题不能为空", trigger: "blur" }
        ],
        noticeType: [
          { required: true, message: "待办类型不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  // mounted() {
  //   this.$nextTick(() => {
  //     this.setTableHeight();
  //   });
  // },
  activated(){
    this.$nextTick(() => {
      this.setTableHeight();
    });
    this._resizeHandler = debounce(() => {
      this.$nextTick(() => {
        this.setTableHeight();
      });
    }, 100);
    window.addEventListener("resize", this._resizeHandler, false);
  },
  deactivated(){
    window.removeEventListener("resize", this._resizeHandler, false);
    // console.log(789);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this._resizeHandler, false);
  },
  methods: {
    setTableHeight() {
      let formHeight = this.$refs.queryForm.$el.clientHeight;
      // console.log(formHeight);
      let tempHeight = window.innerHeight - formHeight - 200;
      this.availableHeight = parseInt(tempHeight);
    },
    /** 查询待办列表 */
    getList() {
      this.loading = true;
      messageListTaskMessage(this.queryParams).then(response => {
        this.messageList = response.rows;
        this.total = response.total;
        this.loading = false;
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
        noticeId: undefined,
        noticeTitle: undefined,
        noticeType: undefined,
        noticeContent: undefined,
        status: "0"
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
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.noticeId)
      this.single = selection.length!=1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加待办";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const noticeId = row.noticeId || this.ids
      getNotice(noticeId).then(response => {
        this.form = response;
        this.open = true;
        this.title = "修改待办";
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.noticeId != undefined) {
            updateNotice(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addNotice(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      let noticeIds = row.noticeId || this.ids
      if (!Array.isArray(noticeIds)) {
        noticeIds = [noticeIds];
      }
      this.$modal.confirm('是否确认删除待办编号为"' + noticeIds + '"的数据项？').then(function() {
        return delNotice(noticeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
