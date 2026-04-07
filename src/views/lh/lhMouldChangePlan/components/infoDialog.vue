<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.factoryCode')" prop="factoryCode">
            <el-select
              v-model="form.factoryCode"
              :placeholder="$t('common.rule.select')"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.biz_factory_name"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.lhResultBatchNo')" prop="lhResultBatchNo">
            <el-input v-model="form.lhResultBatchNo" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.orderNo')" prop="orderNo">
            <el-input v-model="form.orderNo" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.planDate')" prop="planDate">
            <el-date-picker
              v-model="form.planDate"
              type="date"
              placeholder="选择日期"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.scheduleDate')" prop="scheduleDate">
            <el-date-picker
              v-model="form.scheduleDate"
              type="date"
              placeholder="选择日期"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.planOrder')" prop="planOrder">
            <el-input-number v-model="form.planOrder" :placeholder="$t('common.rule.input')" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.leftRightMould')" prop="leftRightMould">
            <el-input v-model="form.leftRightMould" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.lhMachineCode')" prop="lhMachineCode">
            <el-select
              v-model="form.lhMachineCode"
              :placeholder="$t('common.rule.select')"
              filterable
              clearable
              style="width: 100%"
              @focus="handleMachineFocus"
              @change="handleMachineChange"
              :loading="machineLoading"
            >
              <el-option
                v-for="item in machineOptions"
                :key="item.machineCode"
                :label="item.machineCode"
                :value="item.machineCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.lhMachineName')" prop="lhMachineName">
            <el-input v-model="form.lhMachineName" :placeholder="$t('common.rule.input')" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.beforeMaterialCode')" prop="beforeMaterialCode">
            <el-select
              v-model="form.beforeMaterialCode"
              :placeholder="$t('common.rule.select')"
              filterable
              clearable
              style="width: 100%"
              @focus="handleBeforeMaterialFocus"
              @change="handleBeforeMaterialChange"
              :loading="beforeMaterialLoading"
            >
              <el-option
                v-for="item in beforeMaterialOptions"
                :key="item.materialCode"
                :label="item.materialCode"
                :value="item.materialCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.beforeMaterialDesc')" prop="beforeMaterialDesc">
            <el-input v-model="form.beforeMaterialDesc" :placeholder="$t('common.rule.input')" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.changeMouldType')" prop="changeMouldType">
            <el-select
              v-model="form.changeMouldType"
              :placeholder="$t('common.rule.select')"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.CHANGE_MOULD_TYPE"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.afterMaterialCode')" prop="afterMaterialCode">
            <el-select
              v-model="form.afterMaterialCode"
              :placeholder="$t('common.rule.select')"
              filterable
              clearable
              style="width: 100%"
              @focus="handleAfterMaterialFocus"
              @change="handleAfterMaterialChange"
              :loading="afterMaterialLoading"
            >
              <el-option
                v-for="item in afterMaterialOptions"
                :key="item.materialCode"
                :label="item.materialCode"
                :value="item.materialCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.afterMaterialDesc')" prop="afterMaterialDesc">
            <el-input v-model="form.afterMaterialDesc" :placeholder="$t('common.rule.input')" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.changeTime')" prop="changeTime">
            <el-date-picker
              v-model="form.changeTime"
              type="datetime"
              placeholder="选择时间"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.mouldCode')" prop="mouldCode">
            <el-input v-model="form.mouldCode" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.isRelease')" prop="isRelease">
            <el-select
              v-model="form.isRelease"
              :placeholder="$t('common.rule.select')"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.IS_RELEASE"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhMouldChangePlan.mouldStatus')" prop="mouldStatus">
            <el-select
              v-model="form.mouldStatus"
              :placeholder="$t('common.rule.select')"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.biz_yes_no"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="24">
          <el-form-item :label="$t('common.remark')" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { editLhMouldChangePlan, getMachineList, getMaterialList } from "@/api/lh/lhMouldChangePlan";

export default {
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      beforeMaterialLoading: false,
      afterMaterialLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineOptions: [],
      beforeMaterialOptions: [],
      afterMaterialOptions: [],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhResultBatchNo: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        orderNo: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        mouldCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
  },
  methods: {
    async remoteMachineMethod(query) {
      this.machineLoading = true;
      try {
        const res = await getMachineList({
          machineCode: query || "",
          pageSize: 10,
        });
        this.machineOptions = res || [];
        console.log('machineOptions', this.machineOptions);
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineOptions.length === 0) {
        this.remoteMachineMethod("");
      }
    },
    async remoteBeforeMaterialMethod(query) {
      this.beforeMaterialLoading = true;
      try {
        const res = await getMaterialList({
          materialCode: query || "",
          pageSize: 10,
        });
        this.beforeMaterialOptions = res || [];
        console.log('beforeMaterialOptions', this.beforeMaterialOptions);
      } catch (error) {
        console.log(error);
      } finally {
        this.beforeMaterialLoading = false;
      }
    },
    handleBeforeMaterialFocus() {
      if (this.beforeMaterialOptions.length === 0) {
        this.remoteBeforeMaterialMethod("");
      }
    },
    async remoteAfterMaterialMethod(query) {
      this.afterMaterialLoading = true;
      try {
        const res = await getMaterialList({
          materialCode: query || "",
          pageSize: 10,
        });
        this.afterMaterialOptions = res || [];
        console.log('afterMaterialOptions', this.afterMaterialOptions);
      } catch (error) {
        console.log(error);
      } finally {
        this.afterMaterialLoading = false;
      }
    },
    handleAfterMaterialFocus() {
      if (this.afterMaterialOptions.length === 0) {
        this.remoteAfterMaterialMethod("");
      }
    },
    handleMachineChange(val) {
      if (val) {
        const item = this.machineOptions.find(i => i.machineCode === val);
        if (item) {
          this.$set(this.form, 'lhMachineName', item.machineName || val);
        }
      } else {
        this.$set(this.form, 'lhMachineName', '');
      }
    },
    handleBeforeMaterialChange(val) {
      if (val) {
        const item = this.beforeMaterialOptions.find(i => i.materialCode === val);
        if (item) {
          this.$set(this.form, 'beforeMaterialDesc', item.materialDesc || val);
        }
      } else {
        this.$set(this.form, 'beforeMaterialDesc', '');
      }
    },
    handleAfterMaterialChange(val) {
      if (val) {
        const item = this.afterMaterialOptions.find(i => i.materialCode === val);
        if (item) {
          this.$set(this.form, 'afterMaterialDesc', item.materialDesc || val);
        }
      } else {
        this.$set(this.form, 'afterMaterialDesc', '');
      }
    },
    async save() {
      try {
        this.loading = true;
        const res = await editLhMouldChangePlan(this.form);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.lhMachineCode) {
          this.machineOptions = [
            {
              machineCode: data.lhMachineCode,
              machineName: data.lhMachineName || data.lhMachineCode,
            },
          ];
        }
        if (data.beforeMaterialCode) {
          this.beforeMaterialOptions = [
            {
              materialCode: data.beforeMaterialCode,
            },
          ];
        }
        if (data.afterMaterialCode) {
          this.afterMaterialOptions = [
            {
              materialCode: data.afterMaterialCode,
            },
          ];
        }
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
        this.machineOptions = [];
        this.beforeMaterialOptions = [];
        this.afterMaterialOptions = [];
      }
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
      this.beforeMaterialOptions = [];
      this.afterMaterialOptions = [];
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save();
        }
      });
    },
  },
};
</script>
