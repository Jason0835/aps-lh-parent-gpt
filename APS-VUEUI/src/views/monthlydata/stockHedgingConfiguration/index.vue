<template>
  <header-container class="stockHedgingConfiguration" v-loading="loading">
    <template #header>
      <el-button type="primary" :disabled="saveDisabled" @click="handleSave"
        >保存</el-button
      >
      <el-button type="primary" plain @click="handleRefresh">刷新</el-button>
    </template>
    <section class="content">
      <el-card class="container">
        <div slot="header" class="clearfix">
          <span>第一排产顺序</span>
        </div>
        <draggable
          v-if="first.length !== 0"
          v-model="first"
          class="draggable-list"
        >
          <transition-group type="transition" name="flip-list">
            <div
              v-for="(item, index) in first"
              :key="item.optionCode"
              class="list-item"
              :class="{ dragging: drag }"
            >
              <div class="item-content">
                <i class="handle fas fa-grip-lines"></i>
                <span class="text">{{ item.optionName }}</span>
                <span class="index">#{{ index + 1 }}</span>
              </div>
            </div>
          </transition-group>
        </draggable>
        <el-empty v-else />
      </el-card>

      <el-card class="container">
        <div slot="header" class="clearfix">
          <span>第二排产顺序</span>
        </div>
        <draggable v-if="second.length" v-model="second" class="draggable-list">
          <transition-group type="transition" name="flip-list">
            <div
              v-for="(item, index) in second"
              :key="item.optionCode"
              class="list-item"
              :class="{ dragging: drag }"
            >
              <div class="item-content">
                <i class="handle fas fa-grip-lines"></i>
                <span class="text">{{ item.optionName }}</span>
                <span class="index">#{{ index + 1 }}</span>
              </div>
            </div>
          </transition-group>
        </draggable>
        <el-empty v-else />
      </el-card>
      <!-- <div class="container">
        <draggable v-model="list" class="draggable-list">
          <transition-group type="transition" name="flip-list">
            <div
              v-for="(item, index) in list"
              :key="item.id"
              class="list-item"
              :class="{ dragging: drag }"
            >
              <div class="item-content">
                <i class="handle fas fa-grip-lines"></i>
                <span class="text">{{ item.text }}</span>
                <span class="index">#{{ index + 1 }}</span>
              </div>
            </div>
          </transition-group>
        </draggable>
      </div> -->
    </section>
  </header-container>
</template>

<script>
import draggable from "vuedraggable";

import {
  stockHedgingConfigurationList,
  saveStockHedgingConfiguration,
} from "@/api/monthplan/businessSortConfiguration";

export default {
  name: "StockHedgingConfiguration",
  components: { draggable },
  data() {
    return {
      loading: false,
      drag: false,
      first: [],
      second: [],
    };
  },
  computed: {
    saveDisabled: function () {
      return this.second.length === 0 && this.first.length === 0;
    },
  },

  methods: {
    async getConfig() {
      try {
        this.loading = true;
        const res = await stockHedgingConfigurationList();
        this.first = res["1"];
        this.second = res["2"];
        this.$nextTick(() => {
          this.loading = false;
        });
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async save(params) {
      try {
        this.loading = true;

        console.log(JSON.stringify(params));

        const res = await saveStockHedgingConfiguration(params);
        console.log(res);

        this.getConfig();
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    handleSave() {
      const params = {
        firstStockHedgingSortConfigurations: this.first.map((row, index) => {
          return {
            ...row,
            priority: index + 1,
          };
        }),
        secondStockHedgingSortConfigurations: this.second.map((row, index) => {
          return {
            ...row,
            priority: index + 1,
          };
        }),
      };

      this.save(params);
    },
    handleRefresh() {
      this.getConfig();
    },
  },
  activated() {
    this.getConfig();
  },
};
</script>

<style lang='scss' scoped>
.stockHedgingConfiguration {
  .content {
    display: flex;
    justify-content: space-evenly;
  }
}

.container {
  max-width: 800px;
  min-width: 400px;
  margin: 20px;
  /* margin: 2rem auto;
  padding: 0 20px; */
}

.draggable-list {
  /* background: linear-gradient(145deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05); */
}

.list-item {
  background: white;
  margin: 12px 0;
  padding: 18px 25px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: move;
}

.list-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.12);
}

.item-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.handle {
  color: #adb5bd;
  font-size: 1.2rem;
  transition: color 0.2s;
  cursor: grab;
}

.handle:active {
  cursor: grabbing;
}

.text {
  flex-grow: 1;
  color: #495057;
  font-size: 0.9rem;
}

.index {
  color: #868e96;
  font-weight: 500;
  font-size: 0.8rem;
}

.flip-list-move {
  transition: transform 0.6s ease;
}

.dragging {
  opacity: 0.8;
  background: #f8f9fa;
  border: 2px dashed #4dabf7;
}
</style>
