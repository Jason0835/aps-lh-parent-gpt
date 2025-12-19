<template>
    <div class="screen">
        <div class="screen-flex" :style="{
            transform: `scale(${size.scale})`,
            position: 'absolute',
            //top: `${size.top}px`,
            //left: `${size.left}px`,
            '--extraY': `${size.extraY}px`,
            '--extraX': `${size.extraX}px`,
        }">
            <div class="header">
                <img class="header-bg" height="98" :width="1000" src="@/assets/largescreen/title_bg.png">
                <div class="header-title">
                    APS
                </div>
            </div>
            <div class="content">
                <div class="top-layer">
                    <div class="top-left-block">
                        <div class="data-block wide">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/date.svg">
                                <span class="white-shadow-text data-label-text">今年总计划量</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                        <div class="data-block wide">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/target.svg">
                                <span class="white-shadow-text data-label-text">今年完成量</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                    </div>
                    <div class="top-center-block">
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/date.svg">
                                <span class="white-shadow-text data-label-text">本月总计划量</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/target.svg">
                                <span class="white-shadow-text data-label-text">本月完成量</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/data.svg">
                                <span class="white-shadow-text data-label-text">计划达成率</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}%</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                    </div>
                    <div class="top-right-block">
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/date.svg">
                                <span class="white-shadow-text data-label-text">本月总规格数</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/target.svg">
                                <span class="white-shadow-text data-label-text">本月规格完成量</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                        <div class="data-block">
                            <div class="label">
                                <img src="@/assets/largescreen/icon/data.svg">
                                <span class="white-shadow-text data-label-text">规格完成率</span>
                            </div>
                            <div>
                                <template v-if="totalPlanCount.value">
                                    <span class="blue-gradient-text data-value-text"
                                        :style="{ minWidth: totalPlanCount.value.toString().length * 15 + 'px' }">
                                        <countTo :start-val="0" :end-val="totalPlanCount.value" :duration="2000"
                                            :decimals="totalPlanCount.unit ? 2 : 0" />
                                    </span>
                                    <span class="blue-gradient-text data-unit-text">{{ totalPlanCount.unit }}%</span>
                                </template>
                                <span v-else class="blue-gradient-text data-empty-text">-</span>
                            </div>
                        </div>
                    </div>

                </div>
                <div class="middle-layer">
                    <div class="processes" v-for="(item,index) in productionProcesses" :key="index" @click="handleToPage(item.url)">
                        <div class="arrow-img" v-if="index != 0">
                            <img  src="@/assets/largescreen/arrow.svg">
                        </div>
                        <div class="processes-block">
                            <div class="processes-img">
                                <img :src="item.img">
                            </div>
                            <div class="processes-name">{{ item.title }}</div>
                        </div>
                        

                    </div>

                </div>
                
                <div class="bottom-layer">
                    <div class="bottom-left-block">
                        <div class="block-title">
                            <span class="grass-gradient-text">订单接单情况</span>
                        </div>
                        <div class="chart-content">
                            <LineChart :chart-data="orderChart" :height="320 + (size.extraY / 3) + 'px'" />
                        </div>
                    </div>
                    <div class="bottom-center-block">
                        <div class="block-title">
                            <span class="grass-gradient-text">各工序完成情况</span>
                        </div>
                        <div class="table-content">
                            <ScrollTable :columns="completeColumns" :data="completeData" :height="340 + (size.extraY / 3) + 'px'"/>
                        </div>
                    </div>
                    <div class="bottom-right-block">
                        <div class="block-title">
                            <span class="grass-gradient-text">工厂设备情况</span>
                        </div>
                        <div class="table-content">
                            <div class="device-block" v-for="(item,index) in deviceList" :key="index">
                                <div class="device-label white-shadow-text">{{item.label}}</div>
                                <div class="device-value blue-gradient-text">{{item.value}}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</template>
<script>
import LineChart from '@/views/largescreen/components/LineChart'
import ScrollTable from '@/views/largescreen/components/ScrollTable'
import countTo from 'vue-count-to'
import VueSeamlessScroll from 'vue-seamless-scroll'
export default {
    name: 'largescreenIndex',
    components: {
        LineChart,
        ScrollTable,
        VueSeamlessScroll,
        countTo
    },

    data() {
        return {
            //
            size: {
                height: 1080,
                width: 1920,
                scale: 1,
                top: 0,
                left: 0
            },

            totalPlanCount: {
                value: 100,
                unit: '万'
            },

            productionProcesses:[
                {
                    img: require('@/assets/largescreen/factory/控制中心.svg'),
                    title: '月度计划',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/电机.svg'),
                    title: '硫化',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/加工中心.svg'),
                    title: '成型',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/空间焊臂.svg'),
                    title: '胎面',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/CNC.svg'),
                    title: '胎侧',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/激光切割机.svg'),
                    title: '内衬',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/折弯机.svg'),
                    title: '胎圈',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/对刀仪机器人.svg'),
                    title: '钢丝圈',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/压铸件.svg'),
                    title: '钢带压延',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/盘式制动器.svg'),
                    title: '纤维压延',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/汽车发动机.svg'),
                    title: '钢丝斜裁',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/激光切割机.svg'),
                    title: '纤维直裁',
                    url: '#',
                },
                {
                    img: require('@/assets/largescreen/factory/控制中心2.svg'),
                    title: '密炼',
                    url: '#',
                }
            ],

            orderChart: {
            xAxisData: ['202501','202502','202503'],
            yAxisName: '',
            seriesList:[
                {
                    seriesName: '计划订单量',
                    yAxisData: [111,222,333],
                    colors: {
                        dot: '#093F83',
                        dotBorder: '#39CEE6',
                        line: '#45EFFF',
                        area0: '#00deff',
                        area1: '#0B3773'
                    },
                },
                {
                    seriesName: '备货量',
                    yAxisData: [333,111,666],
                    colors: {
                        dot: '#093F83',
                        dotBorder: '#c0d61f',
                        line: '#c0d61f',
                        area0: '#c8ff00',
                        area1: '#0B3773'
                    },
                }
            ]
            },

            completeColumns: [
                {
                    prop: 'xxx',
                    label: '工序',
                    width: 40
                },
                {
                    prop: 'yyy',
                    label: '计划量',
                    width: 40
                },
                {
                    prop: 'zzz',
                    label: '完成量',
                    width: 40
                },
                {
                    prop: 'qqq',
                    label: '达成率',
                    width: 80
                }
            ],
            completeData:[
                {
                    xxx:1,
                    yyy:3,
                    zzz:5,
                    qqq:99
                },
                {
                    xxx:1,
                    yyy:3,
                    zzz:5,
                    qqq:99
                },
                {
                    xxx:1,
                    yyy:3,
                    zzz:5,
                    qqq:99
                },
                {
                    xxx:1,
                    yyy:3,
                    zzz:5,
                    qqq:99
                },
                {
                    xxx:1,
                    yyy:3,
                    zzz:5,
                    qqq:99
                }
            ],

            deviceList: [
                {
                    label:'可用模具数',
                    value: '111',
                },
                {
                    label:'硫化机台数',
                    value: '222',
                },{
                    label:'成型机台数',
                    value: '333',
                },{
                    label:'压出机台数',
                    value: '444',
                },{
                    label:'裁断机台数',
                    value: '555',
                },{
                    label:'压延机台数',
                    value: '666',
                },{
                    label:'密炼机台数',
                    value: '777',
                }
            ]

            

        }
    },
    computed: {

    },
    created() {

        this.calcSize(window.innerWidth, window.innerHeight)
        // 监听窗口大小，缩放屏幕
        window.addEventListener('resize', () => {
            this.calcSize(window.innerWidth, window.innerHeight)
        })

    },
    beforeDestroy() {

    },
    methods: {
        // 计算屏幕缩放
        calcSize(width, height) {
            this.size.height = height
            this.size.width = width
            const xScale = window.innerHeight / 1080
            const yScale = window.innerWidth / 1920
            // console.log(yScale, xScale)

            if (xScale > yScale) {
                // 屏幕更高，上方留出空间，按更小的缩放
                this.size.scale = yScale
                this.size.top = (xScale - yScale) * 1080 / 2
                this.size.left = 0
                this.size.extraY = (xScale - yScale) * 1080 / yScale
                this.size.extraX = 0
            } else {
                // 屏幕更扁，左右方留出空间，按更小的缩放
                this.size.scale = xScale
                this.size.top = 0
                this.size.left = (yScale - xScale) * 1920 / 2
                this.size.extraY = 0
                this.size.extraX = (yScale - xScale) * 1920 / xScale
            }
        },
        //页面跳转
        handleToPage(url){

        }
    }
}
</script>


<style lang="scss" scoped>
@font-face {
    font-family: 'DingTalk';
    src: url('../../../assets/largescreen/fonts/DingTalk JinBuTi.ttf');
}

@font-face {
    font-family: 'DingTalk Sans';
    src: url('../../../assets/largescreen/fonts/DingTalk Sans.ttf');
}

@import '../../../assets/largescreen/style.scss';

.screen {
    background: url(../../../assets/largescreen/bg.png);
}
.processes-block{
    background-image: url(../../../assets/largescreen/block.png);
}
.block-title{
    background-image: url(../../../assets/largescreen/block_title_bg.png);
}
</style>