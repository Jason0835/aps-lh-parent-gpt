<template>
    <div :style="{ height: height, width: width }" />
</template>

<script>
import * as echarts from "echarts";
require('echarts/theme/macarons') // echarts theme
import resize from './resize'
export default {
    name: 'LargeScreenLineChart',
    mixins: [resize],
    props: {
        width: {
            type: String,
            default: '100%'
        },
        height: {
            type: String,
            default: '174px'
        },
        autoResize: {
            type: Boolean,
            default: true
        },
        chartData: {
            type: Object
            //   required: true
        }
    },
    data() {
        return {
            chart: null
        }
    },
    watch: {
        chartData: {
            deep: true,
            handler(val) {
                this.setOptions(val)
            }
        },
        height: {
            deep: true,
            handler(val) {
                this.chart.resize()
            }
        }
    },
    mounted() {
        this.$nextTick(() => {
            this.initChart()
        })
    },
    beforeDestroy() {
        if (!this.chart) {
            return
        }
        this.chart.dispose()
        this.chart = null
    },
    methods: {
        initChart() {
            this.chart = echarts.init(this.$el, 'macarons')
            this.setOptions(this.chartData)
        },
        setOptions({ xAxisData, yAxisData, colors, yAxisName, tooltipFormatter, seriesName, grid, seriesList } = {
            colors: {
                dot: '#093F83',
                dotBorder: '#39CEE6',
                line: '#45EFFF',
                area0: '#00deff',
                area1: '#07214e'
            },
            yAxisName: '次数',
            seriesList: []
        }) {
            let series = []
            let legend = {
                data:[]
            }
            if (seriesList.length > 0) {
                series = seriesList.map(el => {
                    legend.data.push({
                        name:el.seriesName,
                        textStyle:{
                            color: "#FFF",
                            fontSize: 20
                        }
                    })
                    return {
                        name: el.seriesName,
                        itemStyle: {
                            normal: {
                                color: el.colors.dot, // 内容颜色
                                borderColor: el.colors.dotBorder, // 边框颜色
                                borderWidth: 2,
                                lineStyle: {
                                    color: el.colors.line,
                                    width: 2,
                                    shadowColor: 'rgba(0, 0, 0, 1)',
                                    shadowBlur: 10,
                                    shadowOffsetY: 3
                                },
                                textStyle: {
                                    fontFamily: 'D-DIN'
                                }
                            }
                        },
                        areaStyle: {
                            normal: {
                                color: {
                                    type: 'linear',
                                    x: 0,
                                    y: 0,
                                    x2: 0,
                                    y2: 0.8,
                                    colorStops: [
                                        {
                                            offset: 0,
                                            color: el.colors.area0 // 0% 处的颜色
                                        },
                                        {
                                            offset: 1,
                                            color: el.colors.area1 // 100% 处的颜色
                                        }
                                    ],
                                    global: false // 缺省为 false
                                },
                                opacity: 0.3
                            }
                        },
                        symbolSize: 6, // 拐点大小
                        smooth: true,
                        type: 'line',
                        symbol: 'path://A 20,50 0 1,0 1,0',
                        data: el.yAxisData,
                        animationDuration: 2800,
                        animationEasing: 'cubicInOut'
                    }
                })
            } else {
                series = [
                    {
                        name: seriesName,
                        itemStyle: {
                            normal: {
                                color: colors.dot, // 内容颜色
                                borderColor: colors.dotBorder, // 边框颜色
                                borderWidth: 2,
                                lineStyle: {
                                    color: colors.line,
                                    width: 2,
                                    shadowColor: 'rgba(0, 0, 0, 1)',
                                    shadowBlur: 10,
                                    shadowOffsetY: 3
                                },
                                textStyle: {
                                    fontFamily: 'D-DIN'
                                }
                            }
                        },
                        areaStyle: {
                            normal: {
                                color: {
                                    type: 'linear',
                                    x: 0,
                                    y: 0,
                                    x2: 0,
                                    y2: 0.8,
                                    colorStops: [
                                        {
                                            offset: 0,
                                            color: colors.area0 // 0% 处的颜色
                                        },
                                        {
                                            offset: 1,
                                            color: colors.area1 // 100% 处的颜色
                                        }
                                    ],
                                    global: false // 缺省为 false
                                },
                                opacity: 0.3
                            }
                        },
                        symbolSize: 6, // 拐点大小
                        smooth: true,
                        type: 'line',
                        symbol: 'path://A 20,50 0 1,0 1,0',
                        data: yAxisData,
                        animationDuration: 2800,
                        animationEasing: 'cubicInOut'
                    }
                ]
            }
            this.chart.setOption({
                grid: !!grid ? grid : {
                    left: 20,
                    right: 20,
                    bottom: 0,
                    top: 35,
                    containLabel: true
                },
                tooltip: {
                    trigger: 'axis',
                    padding: [5, 10],
                    formatter: tooltipFormatter
                },
                xAxis: {
                    axisLine: {
                        show: false
                    },
                    axisLabel: {
                        show: true,
                        margin: 10,
                        textStyle: {
                            fontSize: 20,
                            fontFamily: 'D-DIN',
                            color: '#FFFFFF'
                        },
                        interval: 0,
                    },
                    data: xAxisData,
                    boundaryGap: 25,
                    axisTick: {
                        show: false
                    }
                },
                yAxis: {
                    name: yAxisName,
                    nameLocation: "end",
                    nameTextStyle: {
                        color: "rgba(255,255,255,0.5)",
                        fontSize: 20,
                        align: 'right',
                        fontFamily: 'DingTalk'
                    },
                    axisLine: {
                        show: false
                    },
                    axisLabel: {
                        show: true,
                        margin: 12,
                        textStyle: {
                            align: 'right',
                            fontSize: 20,
                            color: '#fff'
                        }
                    },
                    axisTick: {
                        show: false
                    },
                    splitLine: {
                        show: true,
                        lineStyle: {
                            type: 'dashed', // y轴分割线类型
                            color: ['#FFFFFF33']
                        }
                    },
                    splitArea: {
                        show: false
                    }
                },
                series: series,
                legend: legend,
            })
        }
    }
}
</script>
<style scoped>
@font-face {
    font-family: 'DingTalk';
    src: url('../../../assets/largescreen/fonts/DingTalk JinBuTi.ttf');
}
</style>
