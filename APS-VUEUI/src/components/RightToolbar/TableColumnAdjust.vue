<template>
  <el-popover placement="bottom" trigger="click" @show="showAdjust">
    <div class="columns-settings" v-if="store">
      <span
        v-if="store.states.leafColumnsLength"
        class="columns-settings-list-title"
        >固定在左侧</span
      >
      <draggable v-model="store.states._columns" @end="forceUpdate">
        <div
          v-if="col.fixed === 'left' || col.fixed === true"
          class="columns-settings-column"
          v-for="col in store.states._columns"
        >
          <i class="el-icon-s-operation"></i>
          <input type="checkbox" v-model="col.visible" @change="forceUpdate" />
          {{ col.label }}

          <div class="columns-settings-column-action">
            <el-tooltip effect="dark" content="固定在列首" placement="top">
              <a
                v-if="col.fixed !== true"
                @click="
                  col.fixed = true;
                  forceUpdate();
                "
                ><i class="el-icon-upload2"></i
              ></a>
            </el-tooltip>
            <el-tooltip effect="dark" content="不固定" placement="top">
              <a
                v-if="col.fixed !== false"
                @click="
                  col.fixed = false;
                  forceUpdate();
                "
                ><i class="el-icon-c-scale-to-original"></i
              ></a>
            </el-tooltip>

            <el-tooltip effect="dark" content="固定在列尾" placement="top">
              <a
                v-if="col.fixed !== 'right'"
                @click="
                  col.fixed = 'right';
                  forceUpdate();
                "
                ><i class="el-icon-download"></i
              ></a>
            </el-tooltip>
          </div>
        </div>
      </draggable>
      <span
        v-if="store.states.leafColumnsLength"
        class="columns-settings-list-title"
        >不固定</span
      >
      <draggable v-model="store.states._columns" @end="forceUpdate">
        <div
          v-if="col.fixed === false"
          class="columns-settings-column"
          v-for="col in store.states._columns"
        >
          <i class="el-icon-s-operation"></i>
          <input type="checkbox" v-model="col.visible" @change="forceUpdate" />
          {{ col.label }}

          <div class="columns-settings-column-action">
            <el-tooltip effect="dark" content="固定在列首" placement="top">
              <a
                v-if="col.fixed !== true"
                @click="
                  col.fixed = true;
                  forceUpdate();
                "
                ><i class="el-icon-upload2"></i
              ></a>
            </el-tooltip>
            <el-tooltip effect="dark" content="不固定" placement="top">
              <a
                v-if="col.fixed !== false"
                @click="
                  col.fixed = false;
                  forceUpdate();
                "
                ><i class="el-icon-c-scale-to-original"></i
              ></a>
            </el-tooltip>

            <el-tooltip effect="dark" content="固定在列尾" placement="top">
              <a
                v-if="col.fixed !== 'right'"
                @click="
                  col.fixed = 'right';
                  forceUpdate();
                "
                ><i class="el-icon-download"></i
              ></a>
            </el-tooltip>
          </div>
        </div>
      </draggable>

      <span
        v-if="store.states.rightFixedLeafColumnsLength"
        class="columns-settings-list-title"
        >固定在右侧</span
      >
      <draggable v-model="store.states._columns" @end="forceUpdate">
        <div
          v-if="col.fixed === 'right'"
          class="columns-settings-column"
          v-for="col in store.states._columns"
        >
          <i class="el-icon-s-operation"></i>
          <input type="checkbox" v-model="col.visible" @change="forceUpdate" />
          {{ col.label }}

          <div class="columns-settings-column-action">
            <el-tooltip effect="dark" content="固定在列首" placement="top">
              <a
                v-if="col.fixed !== true"
                @click="
                  col.fixed = true;
                  forceUpdate();
                "
                ><i class="el-icon-upload2"></i
              ></a>
            </el-tooltip>
            <el-tooltip effect="dark" content="不固定" placement="top">
              <a
                v-if="col.fixed !== false"
                @click="
                  col.fixed = false;
                  forceUpdate();
                "
                ><i class="el-icon-c-scale-to-original"></i
              ></a>
            </el-tooltip>
            <el-tooltip effect="dark" content="固定在列尾" placement="top">
              <a
                v-if="col.fixed !== 'right'"
                @click="
                  col.fixed = 'right';
                  forceUpdate();
                "
                ><i class="el-icon-download"></i
              ></a>
            </el-tooltip>
          </div>
        </div>
      </draggable>
    </div>

    <el-button
      class="menu-icon"
      circle
      slot="reference"
      icon="el-icon-menu"
      size="mini"
    />
  </el-popover>
</template>

<script>
import draggable from "vuedraggable";
export default {
  components: {
    draggable,
  },
  data() {
    return {
      store: undefined,
      table: undefined,
    };
  },
  methods: {
    forceUpdate() {
      this.store.updateColumns();
      this.table.doLayout();
    },
    showAdjust() {
      let parent = this.$parent;
      let table = null;
      while (parent) {
        if (parent?.$refs?.[this.tableRef]) {
          table = parent.$refs[this.tableRef];
          break;
        }
        parent = parent.$parent;
      }
      if (!table) {
        console.error(`表格${this.tableRef}未找到`);
        return;
      }

      this.store = table.store;
      this.table = table;
    },
  },
  name: "TableColumnAdjust",
  props: {
    tableRef: {
      type: String,
    },
  },
};
</script>

<style>
.menu-icon {
  margin-left: 10px;
}
.columns-settings {
  display: flex;
  flex-direction: column;
  width: 160px;
}

.columns-settings-list-title {
  margin-block-start: 6px;
  margin-block-end: 6px;
  padding-inline-start: 24px;
  color: rgba(42, 46, 54, 0.65);
  font-size: 12px;
}

.columns-settings-column {
  position: relative;
  z-index: auto;
  min-height: 24px;
  margin: 0;
  padding: 0 4px;
  color: inherit;
  line-height: 24px;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s, border 0s, line-height 0s, box-shadow 0s;

  display: flex;
  align-items: center;
}

.columns-settings-column:hover .columns-settings-column-action {
  display: block;
}

.columns-settings-column-action {
  margin-left: auto;
  display: none;
}

.columns-settings-column a {
  margin-left: 4px;
  color: rgb(64, 158, 255);
}
</style>
