// frontend/src/pages/Watchlist.tsx
import React, { useEffect, useMemo, useState } from 'react';
import {
    Button,
    Card,
    Col,
    Empty,
    Form,
    Input,
    List,
    message,
    Modal,
    Row,
    Select,
    Space,
    Spin,
    Tag,
    Typography
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import watchlistService, {
    CreateCollectionRequest,
    Page,
    UpdateCollectionRequest,
    UpsertItemRequest,
    WatchlistCollection,
    WatchlistItem
} from '../services/watchlistService';

const { Title, Paragraph, Text } = Typography;

type CollectionsState = Page<WatchlistCollection> & { loading: boolean };

type ItemsState = {
    response: Page<WatchlistItem> | null;
    loading: boolean;
};

const Watchlist: React.FC = () => {
    const [collections, setCollections] = useState<CollectionsState>({
        content: [],
        page: 0,
        size: 20,
        totalPages: 0,
        totalItems: 0,
        hasNext: false,
        hasPrevious: false,
        loading: false
    });
    const [selectedCollection, setSelectedCollection] = useState<WatchlistCollection | null>(null);
    const [items, setItems] = useState<ItemsState>({ response: null, loading: false });
    const [collectionForm] = Form.useForm<CreateCollectionRequest | UpdateCollectionRequest>();
    const [itemForm] = Form.useForm<UpsertItemRequest>();
    const [isCollectionModalVisible, setCollectionModalVisible] = useState(false);
    const [isItemModalVisible, setItemModalVisible] = useState(false);
    const [editingCollection, setEditingCollection] = useState<WatchlistCollection | null>(null);
    const [editingItem, setEditingItem] = useState<WatchlistItem | null>(null);

    const statusColorMap: Record<string, string> = useMemo(() => ({
        WATCHING: 'blue',
        COMPLETED: 'green',
        PLAN_TO_WATCH: 'gold',
        DROPPED: 'red'
    }), []);

    const fetchCollections = async (page = 0, size = 20) => {
        try {
            setCollections(prev => ({ ...prev, loading: true }));
            const response = await watchlistService.listCollections(page, size);
            setCollections({ ...response, loading: false });

            if (response.content.length === 0) {
                setSelectedCollection(null);
                setItems({ response: null, loading: false });
                return;
            }

            const existingSelection = response.content.find(c => c.id === selectedCollection?.id);
            setSelectedCollection(existingSelection ?? response.content[0]);
        } catch (error) {
            console.error(error);
            message.error('Failed to load watchlist collections');
            setCollections(prev => ({ ...prev, loading: false }));
        }
    };

    const fetchItems = async (collection: WatchlistCollection, page = 0, size = 20) => {
        try {
            setItems({ response: null, loading: true });
            const response = await watchlistService.listItems(collection.id, page, size);
            setItems({ response, loading: false });
        } catch (error) {
            console.error(error);
            message.error('Failed to load movies in this collection');
            setItems({ response: null, loading: false });
        }
    };

    useEffect(() => {
        fetchCollections();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (selectedCollection) {
            fetchItems(selectedCollection);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedCollection?.id]);

    const openCreateCollectionModal = () => {
        setEditingCollection(null);
        collectionForm.resetFields();
        setCollectionModalVisible(true);
    };

    const openEditCollectionModal = (collection: WatchlistCollection) => {
        setEditingCollection(collection);
        collectionForm.setFieldsValue({
            name: collection.name,
            description: collection.description,
            isPublic: collection.isPublic
        });
        setCollectionModalVisible(true);
    };

    const handleSaveCollection = async (values: CreateCollectionRequest | UpdateCollectionRequest) => {
        try {
            if (editingCollection) {
                const updated = await watchlistService.updateCollection(editingCollection.id, values);
                setSelectedCollection(updated);
                message.success('Collection updated');
            } else {
                const created = await watchlistService.createCollection(values);
                setSelectedCollection(created);
                message.success('Collection created');
            }
            setCollectionModalVisible(false);
            fetchCollections(collections.page, collections.size);
        } catch (error) {
            console.error(error);
            message.error('Failed to save collection');
        }
    };

    const handleDeleteCollection = (collection: WatchlistCollection) => {
        Modal.confirm({
            title: 'Delete collection',
            content: `Are you sure you want to delete "${collection.name}"?`,
            okType: 'danger',
            onOk: async () => {
                try {
                    await watchlistService.deleteCollection(collection.id);
                    message.success('Collection deleted');
                    if (collection.id === selectedCollection?.id) {
                        setSelectedCollection(null);
                    }
                    fetchCollections(collections.page, collections.size);
                } catch (error) {
                    console.error(error);
                    message.error('Failed to delete collection');
                }
            }
        });
    };

    const openCreateItemModal = () => {
        if (!selectedCollection) return;
        setEditingItem(null);
        itemForm.resetFields();
        setItemModalVisible(true);
    };

    const openEditItemModal = (item: WatchlistItem) => {
        setEditingItem(item);
        itemForm.setFieldsValue({
            movieId: item.movieId,
            status: item.status,
            notes: item.notes
        });
        setItemModalVisible(true);
    };

    const handleSaveItem = async (values: UpsertItemRequest) => {
        if (!selectedCollection) return;
        try {
            if (editingItem) {
                await watchlistService.updateItem(selectedCollection.id, editingItem.movieId, values);
                message.success('Movie updated');
            } else {
                await watchlistService.addItem(selectedCollection.id, values);
                message.success('Movie added');
            }
            setItemModalVisible(false);
            fetchItems(selectedCollection);
        } catch (error) {
            console.error(error);
            message.error('Failed to save movie');
        }
    };

    const handleDeleteItem = (item: WatchlistItem) => {
        if (!selectedCollection) return;
        Modal.confirm({
            title: 'Remove movie',
            content: 'Are you sure you want to remove this movie from the collection?',
            okType: 'danger',
            onOk: async () => {
                try {
                    await watchlistService.deleteItem(selectedCollection.id, item.movieId);
                    message.success('Movie removed');
                    fetchItems(selectedCollection);
                } catch (error) {
                    console.error(error);
                    message.error('Failed to remove movie');
                }
            }
        });
    };

    const renderCollections = () => {
        if (collections.loading) {
            return <Spin />;
        }

        if (collections.content.length === 0) {
            return <Empty description="No collections yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
        }

        return (
            <List
                dataSource={collections.content}
                renderItem={(collection) => (
                    <List.Item
                        key={collection.id}
                        onClick={() => setSelectedCollection(collection)}
                        className={selectedCollection?.id === collection.id ? 'collection-item active' : 'collection-item'}
                        style={{ cursor: 'pointer' }}
                    >
                        <List.Item.Meta
                            title={
                                <Space>
                                    <Text strong>{collection.name}</Text>
                                    <Tag color={collection.isPublic ? 'green' : 'default'}>
                                        {collection.isPublic ? 'Public' : 'Private'}
                                    </Tag>
                                </Space>
                            }
                            description={collection.description}
                        />
                        <Space>
                            <Button
                                type="text"
                                icon={<EditOutlined />}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    openEditCollectionModal(collection);
                                }}
                            />
                            <Button
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handleDeleteCollection(collection);
                                }}
                            />
                        </Space>
                    </List.Item>
                )}
            />
        );
    };

    const renderItems = () => {
        if (!selectedCollection) {
            return <Empty description="Select a collection to view movies" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
        }

        if (items.loading) {
            return <Spin />;
        }

        if (!items.response || items.response.content.length === 0) {
            return <Empty description="No movies in this collection yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
        }

        return (
            <List
                dataSource={items.response.content}
                renderItem={(item) => (
                    <Card
                        key={item.id}
                        type="inner"
                        title={item.movieId}
                        extra={
                            <Space>
                                <Button type="link" onClick={() => openEditItemModal(item)}>
                                    Edit
                                </Button>
                                <Button type="link" danger onClick={() => handleDeleteItem(item)}>
                                    Remove
                                </Button>
                            </Space>
                        }
                        style={{ marginBottom: '1rem' }}
                    >
                        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                            <Tag color={statusColorMap[item.status] || 'default'}>{item.status}</Tag>
                            {item.notes && <Paragraph>{item.notes}</Paragraph>}
                            <Text type="secondary">Added: {new Date(item.addedAt).toLocaleString()}</Text>
                        </Space>
                    </Card>
                )}
            />
        );
    };

    return (
        <div style={{ padding: '2rem', minHeight: '100vh' }}>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                    <div>
                        <Title level={2}>My Watchlists</Title>
                        <Paragraph type="secondary">
                            Organise your favourite movies into collections, track progress, and add personal notes.
                        </Paragraph>
                    </div>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreateCollectionModal}>
                        New Collection
                    </Button>
                </Space>

                <Row gutter={24}>
                    <Col xs={24} md={8}>
                        <Card title="Collections" bordered>
                            {renderCollections()}
                        </Card>
                    </Col>
                    <Col xs={24} md={16}>
                        <Card
                            title={selectedCollection ? selectedCollection.name : 'Select a collection'}
                            bordered
                            extra={selectedCollection && (
                                <Button type="primary" onClick={openCreateItemModal}>
                                    Add Movie
                                </Button>
                            )}
                        >
                            {renderItems()}
                        </Card>
                    </Col>
                </Row>
            </Space>

            <Modal
                title={editingCollection ? 'Edit collection' : 'New collection'}
                open={isCollectionModalVisible}
                onCancel={() => setCollectionModalVisible(false)}
                onOk={() => collectionForm.submit()}
                okText="Save"
            >
                <Form
                    layout="vertical"
                    form={collectionForm}
                    onFinish={handleSaveCollection}
                    initialValues={{ isPublic: true }}
                >
                    <Form.Item
                        label="Name"
                        name="name"
                        rules={[{ required: true, message: 'Please enter a collection name' }]}
                    >
                        <Input placeholder="My sci-fi favourites" />
                    </Form.Item>
                    <Form.Item label="Description" name="description">
                        <Input.TextArea rows={3} placeholder="Optional description" />
                    </Form.Item>
                    <Form.Item
                        label="Visibility"
                        name="isPublic"
                        rules={[{ required: true, message: 'Please choose visibility' }]}
                    >
                        <Select
                            options={[
                                { label: 'Public', value: true },
                                { label: 'Private', value: false }
                            ]}
                        />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={editingItem ? 'Edit movie' : 'Add movie'}
                open={isItemModalVisible}
                onCancel={() => setItemModalVisible(false)}
                onOk={() => itemForm.submit()}
                okText="Save"
            >
                <Form form={itemForm} layout="vertical" onFinish={handleSaveItem}>
                    <Form.Item
                        label="Movie ID"
                        name="movieId"
                        rules={[{ required: true, message: 'Movie ID is required' }]}
                    >
                        <Input placeholder="Movie identifier" disabled={!!editingItem} />
                    </Form.Item>
                    <Form.Item
                        label="Status"
                        name="status"
                        rules={[{ required: true, message: 'Please select a status' }]}
                    >
                        <Select
                            options={[
                                { label: 'Watching', value: 'WATCHING' },
                                { label: 'Completed', value: 'COMPLETED' },
                                { label: 'Plan to Watch', value: 'PLAN_TO_WATCH' },
                                { label: 'Dropped', value: 'DROPPED' }
                            ]}
                        />
                    </Form.Item>
                    <Form.Item label="Notes" name="notes">
                        <Input.TextArea rows={3} placeholder="Optional notes" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Watchlist;