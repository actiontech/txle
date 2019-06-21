<template>
  <div>
    <!-- button -->
    <Button @click="showSubtransaction" type="primary" class="margin-right10">显示子事务</Button>
    <Button @click="hide" type="primary" class="margin-right10">隐藏</Button>
    <Button @click="pauseGts" type="primary" class="margin-right10">暂停全局事务</Button>
    <Button @click="continueGts" type="primary" class="margin-right10">继续全局事务</Button>
    <Button @click="stopGts" type="primary" class="margin-right10">终止全局事务</Button>
    <!-- table -->
    <i-table class="margin-top10" stripe border :columns="columns" :data="data"></i-table>
    <div style="margin: 10px; overflow: hidden">
        <div style="float: right;">
          <Page :total="100" :current="1" @on-change="changePage"></Page>
        </div>
    </div>
  </div>
</template>

<script>
import '../style/global.less'
export default {
  data () {
    return {
      columns: [
        {
          type: 'selection',
          width: 60,
          align: 'center'
        },
        {
          title: '姓名',
          key: 'name'
        },
        {
          title: '年龄',
          key: 'age',
          sortable: true
        },
        {
          title: '状态',
          key: 'status',
          render (row, column, index) {
            const color = row.status == 1 ? 'blue' : row.status == 2 ? 'green' : 'red';
            const text = row.status == 1 ? '构建中' : row.status == 2 ? '构建完成' : '构建失败';
            return `<tag type="dot" color="${color}">${text}</tag>`;
          }
        },
        {
          title: '地址',
          key: 'address'
        }
      ],
      data: [
        {
          name: '王小明',
          age: 18,
          status: 1,
          address: '北京市朝阳区芍药居'
        },
        {
          name: '张小刚',
          age: 25,
          status: 2,
          address: '北京市海淀区西二旗'
        },
        {
          name: '李小红',
          age: 30,
          status: 1,
          address: '上海市浦东新区世纪大道'
        },
        {
          name: '周小伟',
          age: 26,
          status: 2,
          address: '深圳市南山区深南大道'
        }
      ]
    }
  },
  methods: {
    showSubtransaction(){},
    hide (){},
    pauseGts () {},
    continueGts () {},
    stopGts () {},
    changePage (){
      console.log('change-page')
    }
  }
}
</script>
