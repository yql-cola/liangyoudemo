export function categoryParentOptions(tree, excludedId) {
  const result = [{ id: 0, name: '根分类', status: 1 }]
  function visit(nodes, depth, parentDisabled = false) {
    const items = nodes || []
    items.forEach(node => {
      if (node.id === excludedId) return
      const disabled = parentDisabled || node.status === 0
      result.push({ id: node.id, name: `${'　'.repeat(depth)}${node.categoryName || node.name}`, status: disabled ? 0 : 1 })
      visit(node.children, depth + 1, disabled)
    })
  }
  visit(tree, 0)
  return result
}


export function filterCategoryTree(tree, filters = {}) {
  const keyword = String(filters.keyword || '').toLowerCase()
  return (tree || []).reduce((result, node) => {
    const children = filterCategoryTree(node.children, filters)
    const matches = (!keyword || String(node.categoryName || node.name || '').toLowerCase().includes(keyword)) &&
      (filters.status === undefined || filters.status === '' || filters.status === null || node.status === filters.status)
    if (matches || children.length) result.push({ ...node, children })
    return result
  }, [])
}
