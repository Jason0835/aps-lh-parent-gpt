<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
          <el-form-item :label="$t('ui.data.column.lhSpecifyMachine.specCode')" prop="specCode">
            <el-select
              v-model="form.specCode"
              :placeholder="$t('common.rule.select')"
              filterable
              remote
              :remote-method="remoteMaterialMethod"
              :loading="materialLoading"
              clearable
              style="width: 100%"
              @focus="handleMaterialFocus"
            >
              <el-option
                v-for="item in materialOptions"
                :key="item.materialCode"
                :label="item.materialCode"
                :value="item.materialCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhSpecifyMachine.machineCode')" prop="machineCode">
            <el-select
              v-model="form.machineCode"
              :placeholder="$t('common.rule.select')"
              filterable
              remote
              :remote-method="remoteMachineMethod"
              :loading="machineLoading"
              clearable
              style="width: 100%"
              @focus="handleMachineFocus"
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
          <el-form-item :label="$t('ui.data.column.lhSpecifyMachine.lineType')" prop="lineType">
            <el-select
              v-model="form.lineType"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.LINE_TYPE"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhSpecifyMachine.jobType')" prop="jobType">
            <el-select
              v-model="form.jobType"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.JOB_TYPE"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
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
import { editLhSpecifyMachine, getLhMachineList, getMaterialList } from "@/api/lh/lhSpecifyMachine";

export default {
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      machineLoading: false,
      materialLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineOptions: [],
      materialOptions: [],
       rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        specCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.lhSpecifyMachine.modelName");
    },
  },
  methods: {
    async save() {
      try {
        this.loading = true;
        const res = await editLhSpecifyMachine(this.form);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async remoteMachineMethod(query) {
      this.machineLoading = true;
      try {
        const res = await getLhMachineList({ 
          machineCode: query || '',
          pageSize: 10 
        });
        this.machineOptions = res.data || res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineOptions.length === 0) {
        this.remoteMachineMethod('');
      }
    },
    async remoteMaterialMethod(query) {
      this.materialLoading = true;
      try {
        const res = await getMaterialList({ 
          materialCode: query || '',
          pageSize: 10 
        });
        this.materialOptions = res.data || res || [];
      } catch (error) {
        console.log(error);
      } finally {
        this.materialLoading = false;
      }
    },
    handleMaterialFocus() {
      if (this.materialOptions.length === 0) {
        this.remoteMaterialMethod('');
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
        if (data.machineCode) {
          this.machineOptions = [
            {
              machineCode: data.machineCode,
              machineName: data.machineName || data.machineCode,
            },
          ];
        }
        if (data.specCode) {
          this.materialOptions = [
            {
              materialCode: data.specCode,
            },
          ];
        }
      } else {
        this.form = {};
        this.machineOptions = [];
        this.materialOptions = [];
      }
    },
    hide() {
      this.form = {};
      this.machineOptions = [];
      this.materialOptions = [];
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
